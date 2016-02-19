package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.model.util.ImmutableList;

import javax.lang.model.element.Element;

interface ITypeElementImpl extends IElementImpl, Element {
    @Override
    ITypeElementImpl getEnclosingElement();

    @Override
    ImmutableList<? extends ITypeElementImpl> getEnclosedElements();

    @Override
    TypeMirrorImpl asType();
}
