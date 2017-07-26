package cz.cesnet.cloud.sources;

import java.net.URI;

public class Flavour {
	private String id;
	private URI name;
	private int memory;
	private int vcpu;
	private int cpu;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public URI getName() {
		return name;
	}

	public void setName(URI name) {
		this.name = name;
	}

	public int getMemory() {
		return memory;
	}

	public void setMemory(int memory) {
		this.memory = memory;
	}

	public int getVcpu() {
		return vcpu;
	}

	public void setVcpu(int vcpu) {
		this.vcpu = vcpu;
	}

	public int getCpu() {
		return cpu;
	}

	public void setCpu(int cpu) {
		this.cpu = cpu;
	}

	public String toString() {
		return "CPU: " + cpu + "/" + vcpu + ", memory: " + memory + " MB";
	}
}
