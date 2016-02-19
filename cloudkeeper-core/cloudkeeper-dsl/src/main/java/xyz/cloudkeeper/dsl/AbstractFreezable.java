package xyz.cloudkeeper.dsl;

abstract class AbstractFreezable {
    private boolean constructionFinished = false;
    private boolean frozen = false;

    /**
     * Make this module immutable.
     *
     * Subclasses may override this method but <strong>must</strong> call this method first.
     */
    void freeze() {
        requireUnfrozen();

        frozen = true;
    }

    final boolean isFrozen() {
        return frozen;
    }

    /**
     * Throw an exception if this instance has <strong>not</strong> previously been made immutable.
     *
     * @throws IllegalStateException if the {@link #freeze()} method has not been called yet.
     */
    final void requireFrozen() {
        if (!frozen) {
            throw new IllegalStateException(String.format("Tried to access instance of %s for information that is "
                + "only available after the instance has been made immutable.", getClass()));
        }
    }

    /**
     * Throw an exception if this instance has previously been made immutable.
     *
     * @throws IllegalStateException if the {@link #freeze()} method has been called already.
     */
    final void requireUnfrozen() {
        if (frozen) {
            throw new IllegalStateException(String.format("Tried to modify immutable instance of %s.", getClass()));
        }
    }

    void finishConstruction() {
        if (constructionFinished) {
            throw new IllegalStateException(String.format(
                "Tried to finish construction of instance of %s after it had already been marked as finished.",
                getClass()
            ));
        }

        constructionFinished = true;
    }

    final boolean isConstructionFinished() {
        return constructionFinished;
    }
}
