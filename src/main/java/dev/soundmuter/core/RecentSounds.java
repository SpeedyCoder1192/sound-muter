package dev.soundmuter.core;

import java.util.ArrayDeque;
import java.util.List;
import net.minecraft.resources.Identifier;

public class RecentSounds {
	private static final int SIZE = 20;

	private final ArrayDeque<Identifier> sounds = new ArrayDeque<>(SIZE + 1);

	public void add(Identifier id) {
		// usually the same sound as last time (footsteps, rain), skip the shuffle
		if (id.equals(sounds.peekFirst())) {
			return;
		}
		sounds.remove(id);
		sounds.addFirst(id);
		if (sounds.size() > SIZE) {
			sounds.removeLast();
		}
	}

	public List<Identifier> newestFirst() {
		return List.copyOf(sounds);
	}
}
