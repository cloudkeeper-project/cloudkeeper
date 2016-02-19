package xyz.cloudkeeper.model.beans;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

/**
 * Functional option for controlling whether an annotation should be omitted from the CloudKeeper model.
 *
 * <p>An instance of this interface may be passed to {@code from...} static factory methods that construct
 * {@link xyz.cloudkeeper.model.beans.element.MutableAnnotatedConstruct} instances from Java core reflection
 * objects.
 *
 * <p>By default (without this option), annotations are never omitted.
 */
public interface AnnotationCopyOption extends CopyOption {
    /**
     * Returns whether the given annotation should be present in the CloudKeeper model.
     *
     * <p>If this method returns {@code false}, the given annotation will be discarded. Otherwise, the given annotation
     * will be passed to
     * {@link xyz.cloudkeeper.model.beans.element.annotation.MutableAnnotation#fromAnnotation(Annotation, CopyOption...)},
     * and the result will be added to the {@link xyz.cloudkeeper.model.beans.element.MutableAnnotatedConstruct}.
     *
     * <p>Note that this method will be called on all annotations, included those annotated with
     * {@link xyz.cloudkeeper.model.ModelEquivalent}. The argument passed as {@code annotation} is always the
     * original annotation that is directly present on {@code annotatedElement}.
     *
     * @return whether the given annotation should be present in the CloudKeeper model
     */
    boolean isCloudKeeperAnnotation(Annotation annotation, AnnotatedElement annotatedElement);
}
