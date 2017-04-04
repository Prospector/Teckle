package com.elytradev.teckle.common.tile.base;

import com.elytradev.teckle.client.worldnetwork.DumbNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.WorldNetwork;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkNode;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;

/**
 * Created by darkevilmac on 3/28/2017.
 */
public class TileItemNetworkMember extends TileEntity {

    @SideOnly(Side.CLIENT)
    public HashMap<NBTTagCompound, DumbNetworkTraveller> travellers = new HashMap<>();
    private WorldNetworkNode node;

    public void addTraveller(DumbNetworkTraveller traveller) {
        travellers.put(traveller.data, traveller);
    }

    public void removeTraveller(NBTTagCompound data) {
        travellers.remove(data);
    }

    /**
     * Check if this tile can be added to a given network with a neighbour on a specified side.
     *
     * @param network the network to add to
     * @param side    the direction of the neighbour that wants to add
     * @return true if can be added false otherwise.
     */
    public boolean isValidNetworkMember(WorldNetwork network, EnumFacing side) {
        return true;
    }

    public WorldNetworkNode getNode() {
        return node;
    }

    public void setNode(WorldNetworkNode node) {
        this.node = node;
    }

    public WorldNetworkNode getNode(WorldNetwork network) {
        this.node = new WorldNetworkNode(network, pos);

        return node;
    }


    //TODO: Read and write traveller and network data to NBT.

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        return super.writeToNBT(compound);
    }
}
