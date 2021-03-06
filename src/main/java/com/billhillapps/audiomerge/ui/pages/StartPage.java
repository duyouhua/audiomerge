package com.billhillapps.audiomerge.ui.pages;

import java.nio.file.Path;
import java.util.function.Consumer;

import com.billhillapps.audiomerge.processing.MergeManager;
import com.billhillapps.audiomerge.ui.DirectoryList;
import com.billhillapps.audiomerge.ui.DirectoryPicker;
import com.billhillapps.audiomerge.ui.ThemedAlert;

import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class StartPage extends Page {

	private DirectoryList sourceDirList;
	private DirectoryPicker targetDirPicker;

	private Consumer<MergeManager> onStartCallback;

	public StartPage(Stage primaryStage) {
		super();

		Label dirListLabel = new Label("Source directories");
		dirListLabel.getStyleClass().add("title");

		sourceDirList = new DirectoryList(primaryStage);

		Label targetDirLabel = new Label("Target directory");
		targetDirLabel.getStyleClass().add("title");

		targetDirPicker = new DirectoryPicker(primaryStage);

		Button startButton = new Button("Start");
		startButton.getStyleClass().add("start");
		startButton.setMaxWidth(Double.MAX_VALUE);
		startButton.setOnAction(event -> startMerging());

		rootGrid.add(dirListLabel, 0, 0);
		rootGrid.add(sourceDirList, 0, 1);
		rootGrid.add(targetDirLabel, 0, 2);
		rootGrid.add(targetDirPicker, 0, 3);
		rootGrid.add(startButton, 0, 4);
	}

	private void startMerging() {
		if (sourceDirList.hasInvalidPaths()) {
			new ThemedAlert(AlertType.ERROR,
					"There are invalid source paths. Please fix them or remove them from the list.").show();
			return;
		}

		final Path[] sourcePaths = sourceDirList.getChosenDirs().toArray(new Path[] {});
		if (!sourcePathsSanityChecks(sourcePaths))
			return;

		if (!targetDirPicker.isPathValid()) {
			new ThemedAlert(AlertType.ERROR, "Target path is invalid. Please adjust it and try again.").show();
			return;
		}

		final Path targetPath = targetDirPicker.getChosenPath();
		if (!targetPathsSanityChecks(targetPath))
			return;

		MergeManager mergeManager = new MergeManager(targetPath, sourcePaths);
		onStartCallback.accept(mergeManager);
	}

	private boolean sourcePathsSanityChecks(final Path[] sourcePaths) {
		if (sourcePaths.length == 0) {
			new ThemedAlert(AlertType.INFORMATION, "No directory selected, please add at least one.").show();
			return false;
		}

		for (Path sourcePath : sourcePaths)
			if (sourcePath != null && !sourcePath.toFile().isDirectory()) {
				new ThemedAlert(AlertType.WARNING, String.format(
						"Source directory '%s' not found, please specify a different one or remove it.", sourcePath))
								.show();
				return false;
			}

		for (int a = 0; a < sourcePaths.length; a++)
			for (int b = 0; b < sourcePaths.length; b++) {
				if (a == b)
					continue;

				Path pathA = sourcePaths[a];
				Path pathB = sourcePaths[b];
				if (pathA.startsWith(pathB)) {
					new ThemedAlert(AlertType.WARNING,
							String.format("The same directory was chosen more than once, or a subdirectory of it:"
									+ "\n%s\nand\n%s"
									+ "\nPlease remove or change one.", pathA, pathB)).show();
					return false;
				}
			}

		return true;
	}

	private boolean targetPathsSanityChecks(final Path targetPath) {
		if (targetPath == null) {
			new ThemedAlert(AlertType.INFORMATION, "No target directory set, please select one.").show();
			return false;
		}
		if (targetPath != null && !targetPath.toFile().isDirectory()) {
			new ThemedAlert(AlertType.WARNING,
					String.format("Target directory '%s' not found, please specify a different one.", targetPath))
							.show();
			return false;
		}
		if (targetPath.toFile().list().length > 0) {
			new ThemedAlert(AlertType.WARNING,
					"Target directory contains folders or files, please select an empty one.").show();
			return false;
		}

		return true;
	}

	@Override
	public Scene getScene() {
		return this.scene;
	}

	public void onStart(Consumer<MergeManager> onStartCallback) {
		this.onStartCallback = onStartCallback;
	}
}
