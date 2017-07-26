package cz.cesnet.cloud.sources;

import java.util.LinkedList;
import java.util.List;

public class VO {
	private int id;
	private String name;
	private List<Image> images;

	public VO() {
		images = new LinkedList<>();
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void addImage(Image image) {
		images.add(image);
	}

	public List<Image> getImages() {
		return images;
	}

	public int hashCode() {
		return id;
	}

	public boolean equals(Object o) {
		if (o instanceof VO) {
			return (this.id == ((VO) o).id);
		} else {
			return false;
		}
	}

	public String toString() {
		return name;
	}
}
