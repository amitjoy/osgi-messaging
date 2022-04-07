package in.bytehue.messaging.mqtt5.api;

/**
 * This is used to provide the username and password authentication credentials
 * for MQTT simple authentication. Consumer needs to provide the implementation
 * of this API and expose it as a service.
 * <p>
 * <b>Note that</b>, the service is tracked if the {@code simpleAuth}
 * configuration is set to {@code true} and {@code staticAuthCred} is set to
 * {@code false}.
 */
public interface SimpleAuthentication {

	/**
	 * Returns the username to be used for simple authentication
	 *
	 * @return the username
	 * @throws Exception if the dynamic processing throws an exception while
	 *                   calculating the username
	 */
	String username() throws Exception;

	/**
	 * Returns the password to be used for simple authentication
	 *
	 * @return the password
	 * @throws Exception if the dynamic processing throws an exception while
	 *                   calculating the password
	 */
	String password() throws Exception;

}
