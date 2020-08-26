import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


// Text added for testing purposes

public class SendAndReceive {

	static Session session;

	private static String SENDER = "privaxavastqa";
	private static String SENDER_PASS = "Privax1234567890!";
	private static String END_USER_ACCOUNT = "privaxavastqa";
	private static String END_USER_PASS = "Privax1234567890!";
	private static String RECIPIENT = "privaxavastqa@gmail.com"; // abuse-robot@avast.com
																	// abuse-robot@privax.com
	private static final String FILENAME = "HashIdList.txt";
	private static final String TEXT = "EmailComplaint.txt";

	public String nextSessionId() {
		SecureRandom random = new SecureRandom();
		return new BigInteger(130, random).toString(32);
	}

	public boolean ifValidPrint(String ip, String timestamp) {
		boolean result = false;
		boolean validIP = isValidIp(ip);
		boolean validDate = validateDate(timestamp);
		if (!validDate && !validIP) {
			result = false;
			System.out.println("IP format is not correct. Example: 255.225.55.52");
			System.out.println("Date time format is not correct! yyyy-MM-ddTHH:mm:ssZ");
		} else if (!validDate && validIP) {
			result = false;
			System.out.println("Date time format is not correct! yyyy-MM-ddTHH:mm:ssZ");
		} else if (validDate && !validIP) {
			result = false;
			System.out.println("IP format is not correct. Example: 255.34.167.90");
		} else if (validDate && validIP) {
			result = true;
			System.out.println("Successful entry. Generating email...");
		}
		return result;
	}

	public static void main(String[] args) throws IOException, MessagingException, NoSuchAlgorithmException {
		SendAndReceive sr = new SendAndReceive();
		String ip = args[0]; // the first argument - ip
		String timestamp = args[1]; // the second argument - timestamp
		String port = args[2]; // third argument - port

		// print if ip and date valid
		boolean validDateAndIP = sr.ifValidPrint(ip, timestamp);

		// get SubjectID from file
		String lastSubjectID = sr.readSubjectIdFromFile();

		// generate new Subject ID and write in file
		String newSubjectID = sr.getSubjectID();
		System.out.println("New Subject ID is: " + newSubjectID);
		sr.writeSubjectIdInFile(newSubjectID);

		// create Subject and Body of email
		String subject = "Notice of Claimed Infringement - Case ID: " + newSubjectID;
		String body = sr.generateBody(ip, timestamp, newSubjectID, port);

		// send an email from privaxavastqa to privaxavastqa
		if (validDateAndIP) {
			sr.sendEmail(subject, body);
		}

		// list last 5 subjects in INBOX
		ArrayList<Message> messages = sr.getEmails();
		sr.listLastFiveSubjects(messages);

		// check if email was received
		Message emailReceived = sr.ifHMAEmailReceived(messages, lastSubjectID);
		sr.deleteEmail(emailReceived);

		/**
		 * print if email with the last Subject ID is present
		 * sr.ifEmailPresentPrint(messages, lastSubjectID);
		 * sr.readFromFileAndCheckEmail(messages, lastSubjectID);
		 **/

		// ArrayList<Message> updatedMessages = sr.getEmails();
		// sr.readFromFileAndCheckEmail(updatedMessages, newSubjectID);

	}

	public String getEmailBody(Message message) {
		String text = "";
		try {
			text = message.getContent().toString();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		return text;
	}

	public String generateBody(String ip, String timestamp, String subjectID, String port) throws IOException {
		String s;
		BufferedReader br = new BufferedReader(new FileReader(TEXT));
		String text = "";
		while ((s = br.readLine()) != null) {
			text += s.replaceAll("yyyy-MM-ddTHH:mm:ssZ", timestamp).replaceAll("xx.xx.xx.xx", ip)
					.replaceAll("idididididididid", subjectID).replaceAll("ppppp", port) + "\n";
		}
		br.close();
		return text;
	}

	// wait 5 seconds
	public void waitXTime() {
		try {
			TimeUnit.SECONDS.sleep(10);
		} catch (InterruptedException e) {

			e.printStackTrace();
		}
	}

	public void readFromFileAndCheckEmail(ArrayList<Message> messages, String lastSubjectID) {
		if (lastSubjectID == "") {
			System.out.println("File is empty!");
		} else {
			System.out.println(ifEmailWithSubjectIDPresent(messages, lastSubjectID));
		}
	}

	public String ifEmailWithSubjectIDPresent(ArrayList<Message> messages, String subjectID) {
		String email = "No emails!";
		if (messages.size() > 0) {
			for (Message message : messages) {
				try {
					if (message.getSubject().contains(subjectID)) {
						email = "Email with the last Subject ID successfully received: " + subjectID;
					} else {
						email = "Email with the last Subject ID not received yet!";
					}
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}
		return email;
	}

	public Message ifHMAEmailReceived(ArrayList<Message> messages, String subjectID) throws MessagingException {
		Message email = null;
		if (messages.size() > 0) {
			for (Message message : messages) {

				if (message.getSubject().toString().contains("HMA! VPN - File sharing complaint")) {
					System.out.println(message.getSubject().toString());
					String s = getEmailBody(message);
					if (s.contains(subjectID)) {
						System.out.println("Complaint email received by the end user!");
						email = message;
						// message.setFlag(Flags.Flag.DELETED, true);
						// System.out.println("Email successfully deleted");
					}
				}

			}
		}
		return email;
	}

	public void deleteEmail(Message message) {
		if (message != null) {
			try {
				message.setFlag(Flags.Flag.DELETED, true);
				System.out.println("Email successfully deleted");
			} catch (MessagingException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Email not received yet! :(");
		}
	}

	public void listLastFiveSubjects(ArrayList<Message> messages) {
		System.out.println("Number of messages in folder: " + messages.size());
		if (messages.size() > 5) {
			System.out.println("\nThe last 5 subjects in INBOX folder: \n");
			for (int i = messages.size() - 5; i < messages.size(); i++) {
				Message message = messages.get(i);
				printSubject(message);
			}
		} else if (messages.size() > 0 && messages.size() <= 5) {
			System.out.println("The last " + messages.size() + " subjects in INBOX folder: \n");
			for (Message message : messages) {
				printSubject(message);
			}
		} else {
			System.out.println("No emails in INBOX!");
		}

	}

	public void sendEmail(String subject, String body) {
		String from = SENDER;
		String pass = SENDER_PASS;
		String[] to = { RECIPIENT }; // list of recipient email addresses
		sendFromGMail(from, pass, to, subject, body);
		System.out.println("Message sent to HMA!");
	}

	public boolean validateDate(String dateString) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setLenient(false);
		try {
			sdf.parse(dateString);
			return true;
		} catch (ParseException ex) {
			return false;
		}
	}

	private boolean isValidIp(String ip) {
		String[] parts = ip.split("\\.");
		if (parts.length != 4) {
			return false;
		}
		// System.out.println(parts.length);
		// String part1 = parts[0];
		// String part2 = parts[1];
		// String part3 = parts[2];
		// String part4 = parts[3];
		try {
			for (int i = 0; i < parts.length; i++) {
				int part = Integer.parseInt(parts[i]);
				if (part > 255) {
					System.out.println(part);
					return false;
				}
				// System.out.println(parts[i]);
			}
		} catch (NumberFormatException e) {
			System.out.println(e);
			return false;
		}
		return true;
	}

	public ArrayList<Message> getEmails() throws MessagingException {
		Properties props = new Properties();
		props.setProperty("mail.store.protocol", "imaps");
		Store store = null;
		String host = "smtp.gmail.com";
		String from = END_USER_ACCOUNT;
		String pass = END_USER_PASS;
		try {
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.host", host);
			props.put("mail.smtp.user", from);
			props.put("mail.smtp.password", pass);
			props.put("mail.smtp.socketFactory.port", "465");
			props.put("mail.smtp.port", "587");
			props.put("mail.smtp.auth", "true");
			session = Session.getDefaultInstance(props, null);
			store = session.getStore("imaps");
			store.connect(host, from, pass);
		} catch (Exception e) {
			e.printStackTrace();
		}
		ArrayList<Message> FSCmessageList = new ArrayList<Message>();
		try {
			Folder inbox = store.getFolder("inbox");
			inbox.open(Folder.READ_WRITE);

			Message[] messages = inbox.getMessages();
			System.out.println("--------------------------------------");
			int messageCount = inbox.getMessageCount();
			for (int i = 0; i < messageCount; i++) {
				FSCmessageList.add(messages[i]);
			}
			// inbox.close(true);
			// store.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return FSCmessageList;
	}

	public static void closeStore(Store store) throws MessagingException {
		Folder inbox = store.getFolder("inbox");
		inbox.close(true);
		store.close();
	}

	public void printSubject(Message message) {
		try {
			System.out.println("Message subject: " + message.getSubject());
		} catch (MessagingException e) {
			e.printStackTrace();
		}

	}

	private void sendFromGMail(String from, String pass, String[] to, String subject, String body) {
		Properties props = System.getProperties();
		String host = "smtp.gmail.com";
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.user", from);
		props.put("mail.smtp.password", pass);
		props.put("mail.smtp.port", "587");
		props.put("mail.smtp.auth", "true");

		Session session = Session.getDefaultInstance(props);
		MimeMessage message = new MimeMessage(session);

		try {
			message.setFrom(new InternetAddress(from));
			InternetAddress[] toAddress = new InternetAddress[to.length];

			// To get the array of addresses
			for (int i = 0; i < to.length; i++) {
				toAddress[i] = new InternetAddress(to[i]);
			}

			for (int i = 0; i < toAddress.length; i++) {
				message.addRecipient(Message.RecipientType.TO, toAddress[i]);
			}

			message.setSubject(subject);
			message.setText(body);
			Transport transport = session.getTransport("smtp");
			transport.connect(host, from, pass);
			transport.sendMessage(message, message.getAllRecipients());
			transport.close();
		} catch (AddressException ae) {
			ae.printStackTrace();
		} catch (MessagingException me) {
			me.printStackTrace();
		}
	}

	public String getSubjectID() throws NoSuchAlgorithmException {
		String text = nextSessionId();
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		md5.reset();
		md5.update(text.getBytes());
		byte[] digest = md5.digest();
		BigInteger bigInt = new BigInteger(1, digest);
		String hashtext = bigInt.toString(16);
		// Now we need to zero pad it if you actually want the full 32 chars.
		while (hashtext.length() < 32) {
			hashtext = "0" + hashtext;
		}
		// UUID id = UUID.randomUUID();
		// String s = id.toString();
		return hashtext;

	}

	public void writeSubjectIdInFile(String id) {

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILENAME, true))) {
			bw.write(id);
			bw.newLine();
			// no need to close it.
			// bw.close();
			System.out.println("Subject ID written in file");
		} catch (IOException e) {
			e.printStackTrace();

		}
	}

	public String readSubjectIdFromFile() throws IOException {
		String sCurrentLine;
		BufferedReader br = new BufferedReader(new FileReader(FILENAME));
		String lastLine = "";

		while ((sCurrentLine = br.readLine()) != null) {
			// System.out.println(sCurrentLine);
			lastLine = sCurrentLine;
		}
		br.close();
		return lastLine;
	}

}
