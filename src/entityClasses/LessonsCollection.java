package entityClasses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/*******
 * <p> Title: LessonsCollection Class. </p>
 *
 * <p> Description: Holds a complete contributor lesson list or a selected subset.  An ArrayList
 * is used intentionally so empty, one-item, and growing lists are supported without a fixed
 * entry limit.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 */
public class LessonsCollection {

	private final List<LessonLearned> lessons;

	/** Construct an empty collection. */
	public LessonsCollection() {
		lessons = new ArrayList<LessonLearned>();
	}

	/**
	 * @param source specifies the lessons to copy into this collection
	 */
	public LessonsCollection(List<LessonLearned> source) {
		lessons = new ArrayList<LessonLearned>(source);
	}

	/** @return an unmodifiable view of the current lesson list */
	public List<LessonLearned> getLessons() {
		return Collections.unmodifiableList(lessons);
	}

	/** @return the number of lessons in this collection */
	public int size() { return lessons.size(); }

	/** @return true when this collection contains no lessons */
	public boolean isEmpty() { return lessons.isEmpty(); }

	/**********
	 * <p> Method: findById(long id) </p>
	 *
	 * @param id specifies the lesson to locate
	 * @return the matching lesson, or null when it is not present
	 */
	public LessonLearned findById(long id) {
		for (LessonLearned lesson : lessons)
			if (lesson.getId() == id) return lesson;
		return null;
	}

	/**********
	 * <p> Method: filterByTitle(String searchText) </p>
	 *
	 * <p> Description: Build a case-insensitive title subset.  A blank search returns a copy of
	 * the full collection.</p>
	 *
	 * @param searchText specifies the title text to match
	 * @return a new collection containing only matching lessons
	 */
	public LessonsCollection filterByTitle(String searchText) {
		String term = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);
		List<LessonLearned> subset = new ArrayList<LessonLearned>();
		for (LessonLearned lesson : lessons) {
			if (term.isEmpty() || lesson.getTitle().toLowerCase(Locale.ROOT).contains(term))
				subset.add(lesson);
		}
		return new LessonsCollection(subset);
	}
}
