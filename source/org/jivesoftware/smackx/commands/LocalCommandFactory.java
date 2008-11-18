package org.jivesoftware.smackx.commands;

/**
 * A factory for creating local commands. It's useful in cases where instantiation
 * of a command is more complicated than just using the default constructor. For example,
 * when arguments must be passed into the constructor or when using a dependency injection
 * framework. When a LocalCommandFactory isn't used, you can provide the AdHocCommandManager
 * a Class object instead. For more details, see
 * {@link AdHocCommandManager#registerCommand(String, String, LocalCommandFactory)}. 
 *
 * @author Matt Tucker
 */
public interface LocalCommandFactory {

    /**
     * Returns an instance of a LocalCommand.
     *
     * @return a LocalCommand instance.
     * @throws InstantiationException if creating an instance failed.
     * @throws IllegalAccessException if creating an instance is not allowed.
     */
    public LocalCommand getInstance() throws InstantiationException, IllegalAccessException;

}