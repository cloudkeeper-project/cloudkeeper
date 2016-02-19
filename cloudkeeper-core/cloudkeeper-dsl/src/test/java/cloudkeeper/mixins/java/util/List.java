package cloudkeeper.mixins.java.util;

import xyz.cloudkeeper.dsl.ExcludedSuperTypes;
import xyz.cloudkeeper.dsl.TypePlugin;

import java.util.Collection;

@TypePlugin("Java List")
@ExcludedSuperTypes(Collection.class)
public class List { }
