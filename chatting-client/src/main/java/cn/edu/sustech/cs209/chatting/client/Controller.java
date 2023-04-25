package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
// import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class Controller implements Initializable {

	private Socket socket;
	private PrintWriter out;

	private String username;

	private ObservableList<String> users = FXCollections.observableArrayList();

	public String getcurrentUser() {
		return username;
	}

	public Socket getSocket() {
		return socket;
	}

	@FXML
	ListView<Message> chatContentList;
	@FXML
	ListView<Chat> ChatList;

	private static Chat currentchat;
	private static Map<String, Chat> privatechats = new HashMap<>();
	private static Map<Integer, Chat> groupchats = new HashMap<>();

	@FXML
	ListView<String> userList;

	@FXML
	Label currentuser;

	@FXML
	Label currentOnlinecnt;

	private class ServerHandler implements Runnable {

		private BufferedReader in;

		@Override
		public void run() {
			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				char[] input = new char[100000];
				String[] str;
				int len = 0;
				while ((len = in.read(input)) != -1) {
					str = new String(input).substring(0, len - 2).split(";");
					System.out.println(str[0]);
					switch (str[0]) {
						case "UserList":
							ObservableList<String> ulist = FXCollections.observableArrayList();
							for (int i = 1; i < str.length; i++) {
								String s = str[i];
								ulist.add(s);
							}
							userList.setItems(ulist);
							currentOnlinecnt.setText("Online users: " + ulist.size());
							System.out.println("UserCnt: " + ulist.size());
							break;
						case "ServerClosed":
							Alert closed = new Alert(AlertType.WARNING);
							closed.setContentText("Server has closed, exiting");
							closed.show();
							Platform.exit();
							break;

						case "SendMessageP":
							String from = str[1];
							Chat chat = privatechats.get(from);
							Message msg = new Message(Long.valueOf(str[3]), str[1], str[2], str[4]);
							chat.Addmsg(msg);
							ChatList.refresh();
							break;

						case "SendMessageG":
							int id = Integer.valueOf(str[2]);
							Chat gchat = groupchats.get(id);
							Message gmsg = new Message(Long.valueOf(str[3]), str[1], str[2], str[4]);
							gchat.Addmsg(gmsg);
							ChatList.refresh();
							break;
						case "AddPrivate":
							Chat nchat = new Chat(Arrays.asList(username, str[1]), 0);
							nchat.setUpdate(true);
							privatechats.put(str[1], nchat);
							ChatList.getItems().add(nchat);
							break;
						case "AddGroup":
							List<String> S = new ArrayList<>();
							int gid = Integer.valueOf(str[1]);
							for (int i = 2; i < str.length; i++)
								S.add(str[i]);
							Chat c = new Chat(S, gid);
							c.setUpdate(true);
							groupchats.put(gid, c);
							ChatList.getItems().add(c);
							break;
						case "CheckOK":
							username = str[1];
							currentuser.setText("Current User: " + username);
							break;
						case "CheckNO":
							Alert nameused = new Alert(AlertType.WARNING);
							nameused.setContentText("User name has been used! Please change the user name");
							nameused.show();
							Platform.exit();
							break;
						default:
							System.out.println("unrecognized: " + str[0] + str[1]);
							break;
					}
				}
			} catch (IOException e) {
				System.out.println("Error handling server: " + e);
			} finally {
				try {
					System.out.println("socket close");
					socket.close();
				} catch (IOException e) {
					System.out.println("Error closing server socket: " + e);
				}
			}
		}
	}

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		try {
			socket = new Socket("localhost", 2363);
			out = new PrintWriter(socket.getOutputStream(), true);
			ServerHandler T = new ServerHandler();
			Thread t = new Thread(T);
			t.start();
			userList.setCellFactory(new UserCellFactory());
			chatContentList.setCellFactory(new MessageCellFactory());
			ChatList.setCellFactory(new ChatCellFactory());
			ChatList.setItems(FXCollections.observableArrayList());

			Dialog<String> dialog = new TextInputDialog();
			dialog.setTitle("Login");
			dialog.setHeaderText(null);
			dialog.setContentText("Username:");

			Optional<String> input = dialog.showAndWait();
			if (input.isPresent() && !input.get().isEmpty()) {
				out.println(input.get());
				System.out.println(input.get());
			} else {
				Alert invalid = new Alert(AlertType.ERROR);
				invalid.setContentText("Invalid username, exiting");
				invalid.show();
				Platform.exit();
			}
		} catch (IOException e) {
			System.out.println(e);
			Alert failed = new Alert(AlertType.ERROR);
			failed.setContentText("Connect failed.");
			failed.show();
			Platform.exit();
		}
	}

	@FXML
	public void createPrivateChat() {
		AtomicReference<String> user = new AtomicReference<>();

		Stage stage = new Stage();
		ComboBox<String> userSel = new ComboBox<>();

		for (String name : users) {
			if (name != username)
				userSel.getItems().add(name);
		}
		// Done: get the user list from server, the current user's name should be
		// filtered out
		// userSel.getItems().addAll("Item 1", "Item 2", "Item 3");

		Button okBtn = new Button("OK");
		okBtn.setOnAction(e -> {
			user.set(userSel.getSelectionModel().getSelectedItem());
			stage.close();
		});

		HBox box = new HBox(10);
		box.setAlignment(Pos.CENTER);
		box.setPadding(new Insets(20, 20, 20, 20));
		box.getChildren().addAll(userSel, okBtn);
		stage.setScene(new Scene(box));
		stage.showAndWait();

		if (privatechats.containsKey(user.get()))
			chatContentList.setItems(privatechats.get(user.get()).getmsg());
		else {
			Chat chat = new Chat(Arrays.asList(username, user.get()), 0);
			privatechats.put(user.get(), chat);
			out.println("CreatePrivate;" + username + user.get());
		}

		// Done: if the current user already chatted with the selected user, just open
		// the chat with that user
		// Done: otherwise, create a new chat item in the left panel, the title should
		// be the selected user's name
	}

	/**
	 * A new dialog should contain a multi-select list, showing all user's name.
	 * You can select several users that will be joined in the group chat, including
	 * yourself.
	 * <p>
	 * The naming rule for group chats is similar to WeChat:
	 * If there are > 3 users: display the first three usernames, sorted in
	 * lexicographic order, then use ellipsis with the number of users, for example:
	 * UserA, UserB, UserC... (10)
	 * If there are <= 3 users: do not display the ellipsis, for example:
	 * UserA, UserB (2)
	 */
	@FXML
	public void createGroupChat() {
		List<String> res = new ArrayList<>();
		Stage stage = new Stage();
		List<CheckBox> names = new ArrayList<>();
		for (String name : users) {
			if (name != username)
				names.add(new CheckBox(name));
		}
		Button okBtn = new Button("OK");
		okBtn.setOnAction(e -> {
			for (CheckBox c : names)
				if (c.isSelected())
					res.add(c.getText());
			stage.close();
		});

		HBox box = new HBox(50);
		box.setAlignment(Pos.CENTER);
		box.setPadding(new Insets(20, 20, 20, 20));
		names.forEach((s) -> {
			box.getChildren().add(s);
		});
		box.getChildren().add(okBtn);
		stage.setScene(new Scene(box));
		stage.showAndWait();
	}

	/**
	 * Sends the message to the <b>currently selected</b> chat.
	 * <p>
	 * Blank messages are not allowed.
	 * After sending the message, you should clear the text input field.
	 */

	@FXML
	private TextArea inputArea;

	@FXML
	public void doSendMessage() {
		String content = inputArea.getText();
		if (content.length() == 0)
			return;
		if (currentchat.getGroup() > 0) {
			int SendTo = currentchat.getGroup();
			out.println(
					"SendMessageG;" + username + ";" + SendTo + ";" + System.currentTimeMillis() + ";" + content);
		} else {
			String SendTo = currentchat.getMember().get(0).equals(username) ? currentchat.getMember().get(1)
					: currentchat.getMember().get(0);
			out.println(
					"SendMessageP;" + username + ";" + SendTo + ";" + System.currentTimeMillis() + ";" + content);
		}
		inputArea.clear();
		// DONE
	}

	/**
	 * You may change the cell factory if you changed the design of {@code Message}
	 * model.
	 * Hint: you may also define a cell factory for the chats displayed in the left
	 * panel, or simply override the toString method.
	 */
	private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
		@Override
		public ListCell<Message> call(ListView<Message> param) {
			return new ListCell<Message>() {

				@Override
				public void updateItem(Message msg, boolean empty) {
					super.updateItem(msg, empty);
					if (empty || Objects.isNull(msg)) {
						setText(null);
						setGraphic(null);
						return;
					}

					HBox wrapper = new HBox();
					Label nameLabel = new Label(msg.getSentBy() + "  "
							+ new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(msg.getTimestamp()));
					Label msgLabel = new Label(msg.getData());

					nameLabel.setPrefSize(50, 20);
					nameLabel.setWrapText(true);
					nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

					if (username.equals(msg.getSentBy())) {
						wrapper.setAlignment(Pos.TOP_RIGHT);
						wrapper.getChildren().addAll(msgLabel, nameLabel);
						msgLabel.setPadding(new Insets(0, 20, 0, 0));
					} else {
						wrapper.setAlignment(Pos.TOP_LEFT);
						wrapper.getChildren().addAll(nameLabel, msgLabel);
						msgLabel.setPadding(new Insets(0, 0, 0, 20));
					}

					setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
					setGraphic(wrapper);
				}
			};
		}
	}

	private class ChatCellFactory implements Callback<ListView<Chat>, ListCell<Chat>> {
		@Override
		public ListCell<Chat> call(ListView<Chat> param) {
			ListCell<Chat> cell = new ListCell<Chat>() {

				@Override
				public void updateItem(Chat chat, boolean empty) {
					super.updateItem(chat, empty);
					if (empty || Objects.isNull(chat)) {
						setText(null);
						setGraphic(null);
						return;
					}

					Label name;

					if (chat.getGroup() > 0) {
						String S = "";
						for (String s : chat.getMember()) {
							if (S.isEmpty() == false)
								S += ", ";
							S += s;
						}
						name = new Label(S);
					} else
						name = new Label(chat.getMember().get(0).equals(username) ? chat.getMember().get(1) : username);

					name.setPrefSize(50, 20);
					name.setWrapText(false);
					if (chat.getUpdate())
						name.setStyle("-fx-text-fill: red;");
					else
						name.setStyle("-fx-text-fill: black;");
					setContentDisplay(ContentDisplay.LEFT);
					setGraphic(name);
				}
			};
			cell.setOnMouseClicked(event -> {
				if (!cell.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
					cell.getItem().setUpdate(false);
					chatContentList.setItems(cell.getItem().getmsg());
					ChatList.refresh();
				}
			});
			return cell;
		}
	}

	private class UserCellFactory implements Callback<ListView<String>, ListCell<String>> {
		@Override
		public ListCell<String> call(ListView<String> param) {
			return new ListCell<String>() {

				@Override
				public void updateItem(String user, boolean empty) {
					super.updateItem(user, empty);
					if (empty || Objects.isNull(user)) {
						setText(null);
						setGraphic(null);
						return;
					}

					Label name = new Label(user);
					name.setPrefSize(50, 20);
					name.setWrapText(false);
					setContentDisplay(ContentDisplay.LEFT);
					setGraphic(name);
				}
			};
		}
	}

	public void Quit() {
		out.println("UserQuit");
		try {
			socket.getInputStream().close();
			socket.getOutputStream().close();
			socket.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}

}

class Chat {

	private int group;
	private boolean update = false;
	private List<String> member;

	private ObservableList<Message> msg;

	public int getGroup() {
		return group;
	}

	public void setUpdate(boolean up) {
		update = up;
	}

	public boolean getUpdate() {
		return update;
	}

	public List<String> getMember() {
		return member;
	}

	public ObservableList<Message> getmsg() {
		return msg;
	}

	public void Addmsg(Message MSG) {
		msg.add(MSG);
		update = true;
	}

	public Chat(List<String> Member, int ID) {
		this.member = Member;
		Collections.sort(this.member);
		group = ID;
		msg = FXCollections.observableArrayList();
	}

}