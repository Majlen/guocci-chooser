package cz.cesnet.cloud.sources;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

public class Image {
	private URI id;
	private String name;
	private String appDBIdentifier;
	private int appDBID;
	private VO vo;
	private Service service;

	//TODO: ID has to be paired with service & VO
	public URI getId() {
		return id;
	}

	public void setId(URI id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAppDBIdentifier() {
		return appDBIdentifier;
	}

	public void setAppDBIdentifier(String appDBIdentifier) {
		this.appDBIdentifier = appDBIdentifier;
	}

	public int getAppDBID() {
		return appDBID;
	}

	public void setAppDBID(int appDBID) {
		this.appDBID = appDBID;
	}

	public VO getVo() {
		return vo;
	}

	public void setVo(VO vo) {
		this.vo = vo;
	}

	public Service getService() {
		return service;
	}

	public void setService(Service service) {
		this.service = service;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		if (appDBIdentifier != null) {
			return appDBID;
		} else {
			return name.hashCode();
		}
	}

	@Override
	public boolean equals(Object object) {
		if (object != null && object instanceof Image) {
			if (appDBIdentifier != null) {
				if (this.appDBID == ((Image) object).appDBID) {
					return true;
				}
			} else {
				if (this.name.equals(((Image) object).name)) {
					return true;
				}
			}
		}

		return false;
	}
}
