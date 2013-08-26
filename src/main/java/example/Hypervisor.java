package example;

import java.io.Serializable;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonRootName;

/**
 * Hypervisor model, since OpenStack Java SDK doesn't provide 
 */

@SuppressWarnings("serial")
@JsonRootName("hypervisor")
public class Hypervisor implements Serializable {
	
	private String id;
	
	@JsonProperty("hypervisor_hostname")
	private String hostname;

	@JsonProperty("vcpus")
	private Integer vcpus; 
	
	@JsonProperty("vcpus_used")
	private Integer vcpusUsed; 
	
	@JsonProperty("hypervisor_type")
	private String type;
	
	@JsonProperty("hypervisor_version")
	private Integer version;  // WTF is this??? DevStack shows 1000000
	
	@JsonProperty("local_gb")
	private Integer localGB;
	
	@JsonProperty("local_gb_used")
	private Integer localGBUsed;
	
	@JsonProperty("free_disk_gb")
	private Integer freeDiskGB;
	
	@JsonProperty("disk_available_least")
	private Integer diskAvailableLeastGB;
	
	@JsonProperty("memory_mb")
	private Integer memoryMB;
	
	@JsonProperty("memory_mb_used")
	private Integer memoryMBUsed;
	
	@JsonProperty("free_ram_mb")
	private Integer freeMemoryMB; // Don't propagate inconsistent naming
	
	@JsonProperty("running_vms")
	private Integer runningVMs;

	@JsonProperty("current_workload")
	private Integer currentWorkload;
	
	@JsonProperty("cpu_info")
	private String cpuInfoString; // Looks JSON encoded

	
	
	/**
	 * @return the hypervisor id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the hypervisor hostname
	 */
	public String getHostname() {
		return hostname;
	}

	/**
	 * @return the total number of vcpus
	 */
	public Integer getVcpus() {
		return vcpus;
	}
	
	/**
	 * @return the number of vcpus in use
	 */
	public Integer getVcpusUsed() {
		return vcpusUsed;
	}
	
	/**
	 * @return the hypervisor type (e.g. "QEMU")
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * @return the hypervisor version (e.g. DevStack shows 1000000)
	 */
	public Integer getVersion() {
		return version;
	}
	
	/**
	 * @return the total local disk in GB 
	 */
	public Integer getLocalGB() {
		return localGB;
	}
	
	/**
	 * @return the local disk being used in GB 
	 */
	public Integer getLocalGBUsed() {
		return localGBUsed;
	}

	/**
	 * @return the free disk in GB
	 */
	public Integer getFreeDiskGB() {
		return freeDiskGB;
	}
	
	/**
	 * @return at least the amount of disk we know is available in GB
	 */
	public Integer getDiskAvailableLeastGB() {
		return diskAvailableLeastGB;
	}
	
	/**
	 * @return the total memory available in MB
	 */
	public Integer getMemoryMB() {
		return memoryMB;
	}
	
	/**
	 * @return the memory in use in MB
	 */
	public Integer getMemoryMBUsed() {
		return memoryMBUsed;
	}
	
	/**
	 * @return the free memory in MB
	 */
	public Integer getFreeMemoryMB() {
		return freeMemoryMB;
	}
	
	/**
	 * @return the number of running VMs
	 */
	public Integer getRunningVMs() {
		return runningVMs;
	}

	/**
	 * @return the current workload
	 */
	public Integer getCurrentWorkload() {
		return currentWorkload;
	}
	
	/**
	 * Not sure what this is, but looks JSON encoded
	 * @return the CPU info
	 */
	public String getCpuInfoString() {
		return cpuInfoString;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Hypervisor [id=" + id + ", hostname=" + hostname + "]";
	}

}
