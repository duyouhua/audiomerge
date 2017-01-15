package com.billhillapps.audiomerge.ui.pages;

import static com.billhillapps.audiomerge.ui.AudioMergeUI.SPACING;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.function.Consumer;

import com.billhillapps.audiomerge.processing.MergeManager;
import com.billhillapps.audiomerge.ui.ThemedAlert;
import com.billhillapps.audiomerge.ui.choosers.AlbumChooser;
import com.billhillapps.audiomerge.ui.choosers.ArtistChooser;
import com.billhillapps.audiomerge.ui.choosers.SongChooser;
import com.billhillapps.audiomerge.ui.deciders.GuiAlbumDecider;
import com.billhillapps.audiomerge.ui.deciders.GuiArtistDecider;
import com.billhillapps.audiomerge.ui.deciders.GuiSongDecider;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class OperationPage extends Page {

	private final String BUG_REPORT_LINK = "https://github.com/CedricReichenbach/audiomerge/issues/new";

	private final Double CONTENT_WIDTH = 800d;

	private final ArtistChooser artistChooser;
	private final AlbumChooser albumChooser;
	private final SongChooser songChooser;
	private final Label progressLabel;
	private final ProgressBar progressBar;

	private MergeManager mergeManager;

	private final Consumer<MergeManager> onFinishCallback;
	private final HostServices hostServices;

	public OperationPage(Consumer<MergeManager> onFinish, HostServices hostServices) {
		super();

		this.onFinishCallback = onFinish;
		this.hostServices = hostServices;

		rootGrid.setMaxWidth(CONTENT_WIDTH);

		this.artistChooser = new ArtistChooser();
		this.albumChooser = new AlbumChooser();
		this.songChooser = new SongChooser();

		this.progressLabel = new Label();
		progressLabel.getStyleClass().add("progress-label");
		this.progressBar = new ProgressBar(0);
		progressBar.setPrefWidth(CONTENT_WIDTH);
		progressBar.setMaxWidth(Double.MAX_VALUE);
		progressBar.setPadding(new Insets(0, 0, 2 * SPACING, 0));

		VBox progress = new VBox(progressLabel, progressBar);

		rootGrid.add(progress, 0, 0);
		rootGrid.add(artistChooser, 0, 1);
		rootGrid.add(albumChooser, 0, 1);
		rootGrid.add(songChooser, 0, 1);
	}

	// XXX: A bit hacky
	@Override
	protected Pane createRootPane() {
		super.createRootPane();
		// use stack pane for centering
		return new StackPane(rootGrid);
	}

	public void runMergeManager(final MergeManager mergeManager) {
		this.mergeManager = mergeManager;

		mergeManager.setArtistDecider(new GuiArtistDecider(artistChooser));
		mergeManager.setAlbumDecider(new GuiAlbumDecider(albumChooser));
		mergeManager.setSongDecider(new GuiSongDecider(songChooser));

		this.mergeManager.addProgressListener((progress, operation) -> {
			Platform.runLater(() -> {
				progressLabel.setText(operation);
				progressBar.setProgress(progress);
			});
		});

		Thread mergeThread = new Thread(() -> {
			try {
				this.mergeManager.execute();
			} catch (Exception e) {
				mergeFailed(e);
				throw e;
			}

			Platform.runLater(() -> {
				onFinishCallback.accept(this.mergeManager);
			});
		});
		mergeThread.setDaemon(true);
		mergeThread.start();
	}

	private void mergeFailed(Exception exception) {
		Platform.runLater(() -> {
			Label text = new Label(
					String.format("Merging failed.\nProblem: %s\nIf you think this is a bug, please report it here:",
							exception.getMessage()));
			Hyperlink link = buildBugReportLink(exception);
			FlowPane content = new FlowPane(text, link);

			ThemedAlert alert = new ThemedAlert(AlertType.ERROR);
			alert.getDialogPane().setContent(content);
			alert.showAndWait();
			System.exit(1);
		});
	}

	private Hyperlink buildBugReportLink(Exception exception) {
		Hyperlink link = new Hyperlink(BUG_REPORT_LINK);
		String body;
		try {
			body = URLEncoder.encode(String.format("*Message:* `%s`\n*Cause:* `%s`", exception.getMessage(),
					exception.getCause().getClass().getCanonicalName()), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			System.err.println("Failed to build body argument for bug reporting URL");
			e.printStackTrace();
			body = "";
		}
		String args = "?body=" + body;
		link.setOnAction(event -> hostServices.showDocument(BUG_REPORT_LINK + args));
		return link;
	}
}
