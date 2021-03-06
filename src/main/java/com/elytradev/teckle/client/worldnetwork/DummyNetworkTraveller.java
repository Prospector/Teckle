/*
 *    Copyright 2017 Benjamin K (darkevilmac)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.elytradev.teckle.client.worldnetwork;

import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.pathing.WorldNetworkPath;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Created by darkevilmac on 4/2/2017.
 */
public class DummyNetworkTraveller extends WorldNetworkTraveller {

    public DummyNetworkTraveller(NBTTagCompound data, WorldNetworkPath path) {
        super(data);
        this.data = data;
        this.activePath = path;
    }

    @Override
    public void update() {
        // dont.
    }
}
