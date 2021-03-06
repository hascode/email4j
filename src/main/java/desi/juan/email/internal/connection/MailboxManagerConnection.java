/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Juan Desimoni
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package desi.juan.email.internal.connection;

import static java.lang.String.format;

import java.util.Map;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Store;

import desi.juan.email.internal.EmailProtocol;
import desi.juan.email.internal.exception.EmailConnectionException;
import desi.juan.email.internal.exception.EmailException;

/**
 * A connection with a mail server for retrieving emails from an specific folder.
 */
public class MailboxManagerConnection extends AbstractConnection {

  private final Store store;
  private Folder folder;

  /**
   * Creates a new instance of the of the {@link MailboxManagerConnection} secured by TLS.
   */
  public MailboxManagerConnection(EmailProtocol protocol,
                                  String username,
                                  String password,
                                  String host,
                                  int port,
                                  long connectionTimeout,
                                  long readTimeout,
                                  long writeTimeout,
                                  Map<String, String> properties)
  {
    super(protocol, username, password, host, port, connectionTimeout, readTimeout, writeTimeout, properties);
    try {
      this.store = session.getStore(protocol.getName());

      if (username != null && password != null) {
        this.store.connect(username, password);
      } else {
        this.store.connect();
      }
    } catch (MessagingException e) {
      throw new EmailConnectionException(format("Error while acquiring connection with the %s store", protocol), e);
    }
  }

  /**
   * Opens and return the email {@link Folder} of name {@code mailBoxFolder}. The folder can contain Messages, other Folders or
   * both.
   * <p>
   * If there was an already opened folder and a different one is requested the opened folder will be closed and the new one will
   * be opened.
   */
  public synchronized Folder getFolder(String mailBoxFolder, int openMode) {
    try {
      if (folder != null) {
        if (isCurrentFolder(mailBoxFolder) && folder.isOpen() && folder.getMode() == openMode) {
          return folder;
        }
        closeFolder(false);
      }

      folder = store.getFolder(mailBoxFolder);
      folder.open(openMode);
      return folder;
    } catch (MessagingException e) {
      throw new EmailException(format("Error while opening folder [%s]", mailBoxFolder), e);
    }
  }

  /**
   * Closes the current connection folder.
   */
  public synchronized void closeFolder(boolean expunge) {
    try {
      if (folder != null && folder.isOpen()) {
        folder.close(expunge);
      }
    } catch (MessagingException e) {
      throw new EmailException(format("Error while closing mailbox folder %s", folder.getName()), e);
    }
  }

  /**
   * Disconnects {@code this} {@link MailboxManagerConnection} by closing the associated folder and store.
   */
  public synchronized void disconnect() {
    try {
      closeFolder(false);
    } catch (Exception e) {
      //LOGGER.error(format("Error closing mailbox folder [%s] when disconnecting: %s", folder.getName(), e.getMessage()));
    } finally {
      try {
        store.close();
      } catch (Exception e) {
        //LOGGER.error(format("Error closing store when disconnecting: %s", e.getMessage()));
      }
    }
  }

  /**
   * Checks if a mailBoxFolder name is the same name as the current folder.
   */
  private boolean isCurrentFolder(String mailBoxFolder) {
    return folder.getName().equalsIgnoreCase(mailBoxFolder);
  }
}
