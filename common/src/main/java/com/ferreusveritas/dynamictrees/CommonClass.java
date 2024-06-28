package com.ferreusveritas.dynamictrees;

import com.ferreusveritas.dynamictrees.init.BlockInit;
import com.ferreusveritas.dynamictrees.init.EntityInit;
import com.ferreusveritas.dynamictrees.init.ItemInit;
import com.ferreusveritas.dynamictrees.init.TagInit;

public class CommonClass {

    public static void init() {
        ItemInit.loadClass();
        BlockInit.loadClass();
        EntityInit.loadClass();
        TagInit.loadClass();
    }
}