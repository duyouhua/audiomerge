package com.billhillapps.audiomerge.music;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import com.billhillapps.audiomerge.similarity.Decider;

public class Artist implements Entity {

	private final String name;
	private final Decider<Song> songDecider;
	private final EntityBag<Album> albums;

	public Artist(String name, Decider<Album> decider, Decider<Song> songDecider) {
		this.name = name;
		this.songDecider = songDecider;

		albums = new EntityBag<>(decider);
	}

	public String getName() {
		return name;
	}

	public void insertSong(Song song) {
		String albumTitle = song.getAlbumTitle();
		Album album = new Album(albumTitle, songDecider);
		album.addSong(song);

		albums.add(album);
	}

	public String toString() {
		return String.format("an Artist named '%s'", name);
	}

	@Override
	public boolean shallowEquals(Entity other) {
		if (!(other instanceof Artist))
			return false;

		return StringUtils.equals(((Artist) other).name, this.name);
	}

	@Override
	public void mergeIn(Entity other) {
		if (!(other instanceof Artist))
			throw new RuntimeException("Cannot merge two different types");

		Artist otherArtist = (Artist) other;
		albums.addAll(otherArtist.albums);
	}

	public Collection<Album> getAlbums() {
		return albums.asCollection();
	}

	@Override
	public void mergeSimilars() {
		albums.mergeSimilars();
		albums.asCollection().forEach(Album::mergeSimilars);
	}

	@Override
	public void saveTo(Path path) {
		try {
			path.resolve(this.getName());
		} catch (InvalidPathException e) {
			// TODO: Map to valid path
		}

		throw new RuntimeException("Not implemented yet");
		// TODO: Save to sub-directory named like artist
	}
}
