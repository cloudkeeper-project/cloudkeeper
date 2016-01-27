package cloudkeeper.mixins.java.util;

import com.svbio.cloudkeeper.dsl.ExcludedSuperTypes;
import com.svbio.cloudkeeper.dsl.TypePlugin;

import java.util.Collection;

@TypePlugin("Java List")
@ExcludedSuperTypes(Collection.class)
public class List { }
