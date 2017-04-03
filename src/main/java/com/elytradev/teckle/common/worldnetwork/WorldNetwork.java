package com.elytradev.teckle.common.worldnetwork;

import com.elytradev.teckle.common.network.TravellerDataMessage;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

/**
 * Created by darkevilmac on 3/25/2017.
 */
public class WorldNetwork implements ITickable {

    protected static List<WorldNetwork> NETWORKS = new ArrayList<>();
    protected HashMap<BlockPos, WorldNetworkNode> networkNodes = new HashMap<>();
    protected List<WorldNetworkTraveller> travellers = new ArrayList<>();
    protected World world;
    private List<WorldNetworkTraveller> travellersToUnregister = new ArrayList<>();

    public WorldNetwork(World world) {
        this.world = world;

        NETWORKS.add(this);
    }

    @SubscribeEvent
    public static void onTickEvent(TickEvent.WorldTickEvent e) {
        if (NETWORKS.isEmpty() || e.phase.equals(TickEvent.Phase.START) || e.side.isClient())
            return;

        List<WorldNetwork> emptyNetworks = new ArrayList<>();
        for (WorldNetwork network : NETWORKS) {
            if (network.networkNodes.isEmpty()) {
                if (!emptyNetworks.contains(network))
                    emptyNetworks.add(network);

                System.out.println("Found empty network " + network);
                continue;
            }
            if (e.world.equals(network.world))
                network.update();
        }

        for (WorldNetwork emptyNetwork : emptyNetworks) {
            System.out.println("Removing empty network " + emptyNetwork);
            NETWORKS.remove(emptyNetwork);
        }
    }

    public void registerNode(WorldNetworkNode node) {
        System.out.println(this + "/Registering a node, " + node);
        if (!networkNodes.containsKey(node.position))
            networkNodes.put(node.position, node);
        else
            networkNodes.replace(node.position, node);
        node.network = this;
        System.out.println(this + "/Registered node, " + node);
    }

    public void unregisterNode(WorldNetworkNode node) {
        unregisterNodeAtPosition(node.position);
    }

    public void unregisterNodeAtPosition(BlockPos nodePosition) {
        System.out.println(this + "/Unregistering a node at, " + nodePosition);
        if (networkNodes.containsKey(nodePosition))
            networkNodes.remove(nodePosition);
        System.out.println(this + "/Unregistered node at, " + nodePosition);
    }

    public WorldNetworkNode getNodeFromPosition(BlockPos pos) {
        return networkNodes.get(pos);
    }

    public boolean isNodePresent(BlockPos nodePosition) {
        return networkNodes.containsKey(nodePosition);
    }

    public List<WorldNetworkNode> getNodes() {
        return Arrays.asList((WorldNetworkNode[]) networkNodes.values().toArray());
    }

    public List<BlockPos> getNodePositions() {
        return Arrays.asList((BlockPos[]) networkNodes.keySet().toArray());
    }

    public void registerTraveller(WorldNetworkTraveller traveller) {
        travellers.add(traveller);
        traveller.network = this;

        new TravellerDataMessage(TravellerDataMessage.Action.REGISTER, traveller).sendToAllWatching(world, traveller.currentNode.position);
    }

    public void unregisterTraveller(WorldNetworkTraveller traveller) {
        travellersToUnregister.add(traveller);

        if (!traveller.currentNode.isEndpoint())
            new TravellerDataMessage(TravellerDataMessage.Action.UNREGISTER, traveller).sendToAllWatching(world, traveller.currentNode.position);
    }

    public World getWorld() {
        return world;
    }

    public WorldNetwork merge(WorldNetwork otherNetwork) {
        int expectedSize = networkNodes.size() + otherNetwork.networkNodes.size();
        System.out.println("Performing a merge of " + this + " and " + otherNetwork
                + "\n Expecting a node count of " + expectedSize);
        WorldNetwork mergedNetwork = new WorldNetwork(this.world);
        transferNetworkData(mergedNetwork);
        otherNetwork.transferNetworkData(mergedNetwork);
        System.out.println("Completed merge, resulted in " + mergedNetwork);
        return mergedNetwork;
    }

    public void transferNetworkData(WorldNetwork to) {
        List<WorldNetworkTraveller> travellersToMove = new ArrayList<>();
        travellersToMove.addAll(this.travellers);
        List<WorldNetworkNode> nodesToMove = new ArrayList<>();
        nodesToMove.addAll(this.networkNodes.values());

        for (WorldNetworkTraveller traveller : travellersToMove) {
            this.unregisterTraveller(traveller);
            to.registerTraveller(traveller);
        }

        for (WorldNetworkNode node : nodesToMove) {
            this.unregisterNode(node);
            to.registerNode(node);
        }
    }

    /**
     * Checks that the network's connections are fully valid, performs a split if needed.
     */
    public void validateNetwork() {
        // Perform flood fill to validate all nodes are connected. Choose an arbitrary node to current from.

        System.out.println("Performing a network validation.");
        List<List<WorldNetworkNode>> networks = new ArrayList<>();
        HashMap<BlockPos, WorldNetworkNode> uncheckedNodes = new HashMap<>();
        uncheckedNodes.putAll(this.networkNodes);

        while (!uncheckedNodes.isEmpty()) {
            List<WorldNetworkNode> newNetwork = fillFromPos((BlockPos) uncheckedNodes.keySet().toArray()[0], uncheckedNodes);
            for (WorldNetworkNode checkedNode : newNetwork) {
                uncheckedNodes.remove(checkedNode.position);
            }
            networks.add(newNetwork);
        }

        // Only process a split if there's a new network that needs to be formed. RIP old network </3
        if (networks.size() > 1) {
            System.out.println("Splitting a network...");
            //Start from 1, leave 0 as this network.
            for (int networkNum = 1; networkNum < networks.size(); networkNum++) {
                List<WorldNetworkNode> newNetworkData = networks.get(networkNum);
                WorldNetwork newNetwork = new WorldNetwork(this.world);
                for (WorldNetworkNode node : newNetworkData) {
                    this.unregisterNode(node);
                    newNetwork.registerNode(node);

                    // Move travellers if needed.
                    // TODO: Find travellers a new entry point.
                    if (!node.getTravellers().isEmpty()) {
                        for (WorldNetworkTraveller traveller : node.getTravellers()) {
                            traveller.network.unregisterTraveller(traveller);
                            newNetwork.registerTraveller(traveller);
                        }
                    }
                }
            }
        }

        System.out.println("Finished validation, resulted in " + networks.size() + " networks.\n Network sizes follow.");
        for (List<WorldNetworkNode> n : networks) {
            System.out.println(n.size());
        }
    }

    public List<WorldNetworkNode> fillFromPos(BlockPos startAt, HashMap<BlockPos, WorldNetworkNode> knownNodes) {
        List<BlockPos> posStack = new ArrayList<>();
        List<BlockPos> iteratedPositions = new ArrayList<>();
        List<WorldNetworkNode> out = new ArrayList<>();

        posStack.add(startAt);
        iteratedPositions.add(startAt);
        while (!posStack.isEmpty()) {
            BlockPos pos = posStack.remove(0);
            if (knownNodes.containsKey(pos)) {
                System.out.println("Added " + pos + " to out.");
                out.add(knownNodes.get(pos));
            }

            for (EnumFacing direction : EnumFacing.VALUES) {
                if (!iteratedPositions.contains(pos.add(direction.getDirectionVec())) && knownNodes.containsKey(pos.add(direction.getDirectionVec()))) {
                    posStack.add(pos.add(direction.getDirectionVec()));
                    iteratedPositions.add(pos.add(direction.getDirectionVec()));
                }
            }
        }

        return out;
    }

    @Override
    public void update() {
        for (WorldNetworkTraveller traveller : travellers) {
            traveller.update();
        }

        for (WorldNetworkTraveller traveller : travellersToUnregister) {
            travellers.remove(traveller);
        }
        travellersToUnregister.clear();
    }

    @Override
    public String toString() {
        return "WorldNetwork{" +
                "nodeCount=" + networkNodes.size() +
                ", travellerCount=" + travellers.size() +
                ", worldID=" + world.provider.getDimension() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorldNetwork network = (WorldNetwork) o;
        return Objects.equals(networkNodes, network.networkNodes) &&
                Objects.equals(travellers, network.travellers) &&
                Objects.equals(world, network.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkNodes, travellers, world);
    }
}

