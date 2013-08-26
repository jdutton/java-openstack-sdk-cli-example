package example;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Hypervisors model, since OpenStack Java SDK doesn't provide 
 */

@SuppressWarnings("serial")
public class Hypervisors implements Iterable<Hypervisor>, Serializable {

	@JsonProperty("hypervisors")
	private List<Hypervisor> list;

	/**
	 * @return the list
	 */
	public List<Hypervisor> getList() {
		return list;
	}
	
	@Override
	public Iterator<Hypervisor> iterator() {
		return list.iterator();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Hypervisors [list=" + list + "]";
	}

}
