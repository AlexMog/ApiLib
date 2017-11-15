package alexmog.apilib.services;

import java.util.Properties;
import java.util.logging.Level;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import alexmog.apilib.ApiServer;
import alexmog.apilib.managers.ServicesManager.Service;

@Service
public class EmailsService extends BasicWaitAndExecuteService {
	private Transport mTransport;
	private Session mSession;
	private String mFrom;
	private boolean mDeactivated = false;
	
	public MimeMessage generateMime(String subject, String content, String contentType, String...recipients) throws MessagingException {
		MimeMessage message = new MimeMessage(mSession);
		message.setSubject(subject);
        message.setContent(content, contentType);
        for (String recipient : recipients) {
	        message.addRecipient(Message.RecipientType.TO,
		             new InternetAddress(recipient));
        }
		return message;
	}
	
	public void sendEmail(final MimeMessage email) {
		addAction(new Runnable() {
			
			@Override
			public void run() {
				try {
					email.setFrom(new InternetAddress(mFrom));
					mTransport.sendMessage(email,
					        email.getRecipients(Message.RecipientType.TO));
				} catch (MessagingException e) {
					ApiServer.LOGGER.log(Level.WARNING, "Email cannot be sent.", e);
				}
			}
		});
	}
	
	@Override
	public void run() {
		if (mDeactivated) return;
		while (mRunning) {
			mLock.lock();
			try {
				if (mActionMaps.isEmpty()) {
					mCondVar.await();
				}
				mTransport.connect();
				while (mRunning && !mActionMaps.isEmpty()) {
					mActionMaps.pop().run();
				}
				mTransport.close();
			} catch (InterruptedException | MessagingException e) {
				ApiServer.LOGGER.log(Level.SEVERE, "Service", e);
			} finally {
				mLock.unlock();
			}
		}
	}

	@Override
	public void init(Properties config) throws Exception {
		if (config.getProperty("mail.smtp.auth.user") == null) {
			mDeactivated = true;
			return;
		}
        Authenticator auth = new SMTPAuthenticator(config.getProperty("mail.smtp.auth.user"), config.getProperty("mail.smtp.auth.password"));
        mSession = Session.getDefaultInstance(config, auth);

        mTransport = mSession.getTransport();
        mFrom = config.getProperty("mail.from");
	}
	
	private class SMTPAuthenticator extends Authenticator {
		private final String mUsername, mPassword;
		
		public SMTPAuthenticator(String username, String password) {
			mUsername = username;
			mPassword = password;
		}
		
		@Override
        public PasswordAuthentication getPasswordAuthentication() {
           return new PasswordAuthentication(mUsername, mPassword);
        }
    }
}
