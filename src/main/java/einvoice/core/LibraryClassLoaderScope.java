package einvoice.core;

/**
 * Makes the application's class loader the context class loader of the current thread for the duration of a
 * try-with-resources block. The XML, PDF and validation libraries look up their parsers and rule files
 * through the context class loader, which JavaFX threads do not have.
 */
final class LibraryClassLoaderScope implements AutoCloseable {
    private final Thread thread = Thread.currentThread();
    private final ClassLoader original = thread.getContextClassLoader();

    private LibraryClassLoaderScope() {
        thread.setContextClassLoader(LibraryClassLoaderScope.class.getClassLoader());
    }

    /** Switches the context class loader; closing the returned scope restores the previous one. */
    static LibraryClassLoaderScope enter() {
        return new LibraryClassLoaderScope();
    }

    @Override
    public void close() {
        thread.setContextClassLoader(original);
    }
}
