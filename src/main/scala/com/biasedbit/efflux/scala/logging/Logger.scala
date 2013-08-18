package com.biasedbit.efflux.scala.logging
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

object Logger {
  // static methods -------------------------------------------------------------------------------------------------

  /**
   * Return a logger for a given class.
   *
   * @param clazz Class.
   *
   * @return Logger for a class.
   */
  def getLogger(clazz: Class[_]): Logger = {
    return new Logger(clazz);
  }

  def getLogger(name: String): Logger = {
    return new Logger(name);
  }
}
/**
 * Facade for SLF4J's logger.
 * <p/>
 * Offers java 5 syntax support (varargs) and performs some optimisations.
 *
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 */
class Logger(name: String) {

  // internal vars --------------------------------------------------------------------------------------------------

  private val logger: org.slf4j.Logger = LoggerFactory.getLogger(name)

  // constructors ---------------------------------------------------------------------------------------------------

  /**
   * constructor
   *
   * @param clazz Name of the class to log.
   */
  def this(clazz: Class[_]) = this(clazz.getName())

  // public methods -------------------------------------------------------------------------------------------------

  /**
   * Return the name of the logger.
   *
   * @return Name of the logger.
   */
  def getName(): String = this.logger.getName();

  /**
   * Test whether trace logging level is enabled.
   *
   * @return <code>true</code> if trace logging level is enabled, <code>false</code> otherwise.
   */
  def isTraceEnabled(): Boolean = this.logger.isTraceEnabled();

  /**
   * Print a trace message to the logs.
   *
   * @param message Message to be logged.
   */
  def trace(message: String) = this.logger.trace(message);

  /**
   * Print a formatted trace message to the logs, with an arbitrary number of parameters.
   *
   * @param message    Message to be logged.
   * @param parameters Array of parameters.
   */
  def trace(message: String, parameters: Any*) {
    // This is where magic happens and syntax sugar is offered...
    // The interface overrides an Any[] and offers Any* instead!
    // Awesome, ain't it? No. It should come as standard in SLF4J.
    this.logger.trace(message, parameters);
  }

  /**
   * Print a trace message with an exception.
   *
   * @param message   Message to be logged.
   * @param throwable Throwable to be logged.
   */
  def trace(message: String, throwable: Throwable) {
    this.logger.trace(message, throwable);
  }

  /**
   * Print a formatted trace message to the logs, with an arbitrary number of parameters and an exception.
   * <p/>
   * In order for this to work a workaround has to be done, because SLF4J's api only allows for exceptions to be
   * logged alongside with a String, thus disabling formatted message AND exception logging.<br/> Resorting to SLF4J's
   * MessageFormater to format the message with the parameters into a string and then passing that string as the
   * message, it is possible to have the best of both worlds.<br/> In order to avoid incurring the overhead of this
   * process, before doing it, a test to check if the log level is enabled is done.
   * <p/>
   * NOTE: The order of the throwable and Any array is switched due to Java language limitations, but the exception
   * always shows up AFTER the formatted message.
   *
   * @param message    Message to be formatted.
   * @param throwable  Throwable to be logged.
   * @param parameters Parameters to format the message.
   */
  def trace(message: String, throwable: Throwable, parameters: Any*) {
    if (this.logger.isTraceEnabled()) {
      this.logger.trace(MessageFormatter.arrayFormat(message, parameters.toArray.map(x ⇒ x.asInstanceOf[java.lang.Object])).getMessage(), throwable);
    }
  }

  /**
   * Test whether debug logging level is enabled.
   *
   * @return <code>true</code> if debug logging level is enabled, <code>false</code> otherwise.
   */
  def isDebugEnabled() = this.logger.isDebugEnabled();

  /**
   * Print a debug message to the logs.
   *
   * @param message Message to be logged.
   */
  def debug(message: String) = this.logger.debug(message);

  /**
   * Print a formatted debug message to the logs, with an arbitrary number of parameters.
   *
   * @param message    Message to be logged.
   * @param parameters Array of parameters.
   */
  def debug(message: String, parameters: Any*) {
    this.logger.debug(message, parameters.toArray.map(x ⇒ x.asInstanceOf[java.lang.Object]))
  }
  /**
   * Print a formatted debug message to the logs, with an arbitrary number of parameters and an exception.
   * <p/>
   * In order for this to work a workaround has to be done, because SLF4J's api only allows for exceptions to be
   * logged alongside with a String, thus disabling formatted message AND exception logging.<br/> Resorting to SLF4J's
   * MessageFormater to format the message with the parameters into a string and then passing that string as the
   * message, it is possible to have the best of both worlds.<br/> In order to avoid incurring the overhead of this
   * process, before doing it, a test to check if the log level is enabled is done.
   * <p/>
   * NOTE: The order of the throwable and Any array is switched due to Java language limitations, but the exception
   * always shows up AFTER the formatted message.
   *
   * @param message    Message to be formatted.
   * @param throwable  Throwable to be logged.
   * @param parameters Parameters to format the message.
   */
  def debug(message: String, throwable: Throwable, parameters: Any*) {
    if (this.logger.isDebugEnabled()) {
      this.logger.debug(MessageFormatter.arrayFormat(message, parameters.toArray.map(x ⇒ x.asInstanceOf[java.lang.Object])).getMessage(), throwable);
    }
  }

  /**
   * Test whether info logging level is enabled.
   *
   * @return <code>true</code> if info logging level is enabled, <code>false</code> otherwise.
   */
  def isInfoEnabled() = this.logger.isInfoEnabled();

  /**
   * Print a info message to the logs.
   *
   * @param message Message to be logged.
   */
  def info(message: String) {
    this.logger.info(message);
  }

  /**
   * Print a formatted info message to the logs, with an arbitrary number of parameters.
   *
   * @param message    Message to be logged.
   * @param parameters Array of parameters.
   */
  def info(message: String, parameters: Any*) {
    this.logger.info(message, parameters);
  }

  /**
   * Print an info message with an exception.
   *
   * @param message   Message to be logged.
   * @param throwable Throwable to be logged.
   */
  def info(message: String, throwable: Throwable) {
    this.logger.info(message, throwable);
  }

  /**
   * Print a formatted info message to the logs, with an arbitrary number of parameters and an exception.
   * <p/>
   * In order for this to work a workaround has to be done, because SLF4J's api only allows for exceptions to be
   * logged alongside with a String, thus disabling formatted message AND exception logging.<br/> Resorting to SLF4J's
   * MessageFormater to format the message with the parameters into a string and then passing that string as the
   * message, it is possible to have the best of both worlds.<br/> In order to avoid incurring the overhead of this
   * process, before doing it, a test to check if the log level is enabled is done.
   * <p/>
   * NOTE: The order of the throwable and Any array is switched due to Java language limitations, but the exception
   * always shows up AFTER the formatted message.
   *
   * @param message    Message to be formatted.
   * @param throwable  Throwable to be logged.
   * @param parameters Parameters to format the message.
   */
  def info(message: String, throwable: Throwable, parameters: Any*) {
    if (this.logger.isInfoEnabled()) {
      this.logger.info(MessageFormatter.arrayFormat(message, parameters.toArray.map(x ⇒ x.asInstanceOf[java.lang.Object])).getMessage(), throwable);
    }
  }

  /**
   * Test whether warn logging level is enabled.
   *
   * @return <code>true</code> if warn logging level is enabled, <code>false</code> otherwise.
   */
  def isWarnEnabled() = this.logger.isWarnEnabled();

  /**
   * Print a warning message to the logs.
   *
   * @param message Message to be logged.
   */
  def warn(message: String) {
    this.logger.warn(message);
  }

  /**
   * Print a formatted warning message to the logs, with an arbitrary number of parameters.
   *
   * @param message    Message to be logged.
   * @param parameters Array of parameters.
   */
  def warn(message: String, parameters: Any*) {
    this.logger.warn(message, parameters);
  }

  /**
   * Print a warning message with an exception.
   *
   * @param message   Message to be logged.
   * @param throwable Throwable to be logged.
   */
  def warn(message: String, throwable: Throwable) {
    this.logger.warn(message, throwable);
  }

  /**
   * Print a formatted warn message to the logs, with an arbitrary number of parameters and an exception.
   * <p/>
   * In order for this to work a workaround has to be done, because SLF4J's api only allows for exceptions to be
   * logged alongside with a String, thus disabling formatted message AND exception logging.<br/> Resorting to SLF4J's
   * MessageFormater to format the message with the parameters into a string and then passing that string as the
   * message, it is possible to have the best of both worlds.<br/> In order to avoid incurring the overhead of this
   * process, before doing it, a test to check if the log level is enabled is done.
   * <p/>
   * NOTE: The order of the throwable and Any array is switched due to Java language limitations, but the exception
   * always shows up AFTER the formatted message.
   *
   * @param message    Message to be formatted.
   * @param throwable  Throwable to be logged.
   * @param parameters Parameters to format the message.
   */
  def warn(message: String, throwable: Throwable, parameters: Any*) {
    if (this.logger.isWarnEnabled()) {
      this.logger.warn(MessageFormatter.arrayFormat(message, parameters.toArray.map(x ⇒ x.asInstanceOf[java.lang.Object])).getMessage(), throwable);
    }
  }

  /**
   * Test whether error logging level is enabled.
   *
   * @return <code>true</code> if error logging level is enabled, <code>false</code> otherwise.
   */
  def isErrorEnabled() = this.logger.isErrorEnabled();

  /**
   * Print an error message to the logs.
   *
   * @param message Message to be logged.
   */
  def error(message: String) {
    this.logger.error(message);
  }

  /**
   * Print a formatted error message to the logs, with an arbitrary number of parameters.
   *
   * @param message    Message to be logged.
   * @param parameters Array of parameters.
   */
  def error(message: String, parameters: Any*) {
    this.logger.error(message, parameters);
  }

  /**
   * Print an error message with an exception.
   *
   * @param message   Message to be logged.
   * @param throwable Throwable to be logged.
   */
  def error(message: String, throwable: Throwable) {
    this.logger.error(message, throwable);
  }

  /**
   * Print a formatted debug message to the logs, with an arbitrary number of parameters and an exception.
   * <p/>
   * In order for this to work a workaround has to be done, because SLF4J's api only allows for exceptions to be
   * logged alongside with a String, thus disabling formatted message AND exception logging.<br/> Resorting to SLF4J's
   * MessageFormater to format the message with the parameters into a string and then passing that string as the
   * message, it is possible to have the best of both worlds.<br/> In order to avoid incurring the overhead of this
   * process, before doing it, a test to check if the log level is enabled is done.
   * <p/>
   * NOTE: The order of the throwable and Any array is switched due to Java language limitations, but the exception
   * always shows up AFTER the formatted message.
   *
   * @param message    Message to be formatted.
   * @param throwable  Throwable to be logged.
   * @param parameters Parameters to format the message.
   */
  def error(message: String, throwable: Throwable, parameters: Any*) {
    if (this.logger.isErrorEnabled()) {
      this.logger.error(MessageFormatter.arrayFormat(message, parameters.toArray.map(x ⇒ x.asInstanceOf[java.lang.Object])).getMessage(), throwable);
    }
  }
}
