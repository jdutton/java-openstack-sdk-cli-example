package example;

import org.apache.commons.cli.*;

import com.woorea.openstack.base.client.HttpMethod;
import com.woorea.openstack.base.client.OpenStackClientConnector;
import com.woorea.openstack.base.client.OpenStackRequest;
import com.woorea.openstack.base.client.OpenStackResponseException;
import com.woorea.openstack.base.client.OpenStackSimpleTokenProvider;
//import com.woorea.openstack.connector.JerseyConnector; // Override to ignore unknown JSON properties
import com.woorea.openstack.keystone.Keystone;
import com.woorea.openstack.keystone.model.Access;
import com.woorea.openstack.keystone.model.Authentication;
import com.woorea.openstack.keystone.model.Endpoint;
import com.woorea.openstack.keystone.model.Endpoints;
import com.woorea.openstack.keystone.model.Service;
import com.woorea.openstack.keystone.model.Services;
import com.woorea.openstack.keystone.model.Tenant;
import com.woorea.openstack.keystone.model.Tenants;
import com.woorea.openstack.keystone.model.User;
import com.woorea.openstack.keystone.model.Users;
import com.woorea.openstack.keystone.model.authentication.TokenAuthentication;
import com.woorea.openstack.keystone.model.authentication.UsernamePassword;
import com.woorea.openstack.keystone.utils.KeystoneUtils;
import com.woorea.openstack.nova.Nova;
import com.woorea.openstack.nova.api.extensions.HostsExtension;
import com.woorea.openstack.nova.model.Hosts;
import com.woorea.openstack.nova.model.Hosts.Host;
import com.woorea.openstack.nova.model.Server;
import com.woorea.openstack.nova.model.Servers;


public class Cli
{
	/**
	 * Keystone username to authorize (defaults to "admin")
	 */
	private String username = "admin";
	
	/**
	 * Tenant name that is used to authorize with Keystone (defaults to "admin")
	 */
	private String tenantName = "admin";
	
	/**
	 * User password for authenticating with keystone (defaults to "password")
	 */
	private String password = "password";
	
	/**
	 * Keystone API endpoint (e.g. "http://172.16.0.1:35357/v2.0/")
	 * 
	 * Note that this CLI expects to be admin, so wants to use the Keystone Admin API,
	 * which is usually on port 35357, not on port 5000.
	 * 
	 */
	private String apiEndpoint;

	// Client objects shared by multiple methods
	private OpenStackClientConnector connector;
	private Access access;
	private Keystone keystone;
	
	/**
	 * 
	 * @param args
	 */
	public static void main( String[] args )
    {
		Cli app = new Cli();
    	app.parseOptions(args);

    	System.out.println("User " + app.username);
    	System.out.println("Tenant " + app.tenantName);
    	System.out.println("Password " + app.password);
    	System.out.println("Endpoint " + app.apiEndpoint);

    	app.run();
    }
    
    private void parseOptions( String[] args ) {
    	Options options = new Options();
    	options.addOption("u", "user", true, "Username");
    	options.addOption("t", "tenant", true, "Tenant Name");
    	options.addOption("p", "password", true, "Password");
    	options.addOption("a", "api", true, "Keystone Admin API endpoint");
    	
    	CommandLineParser parser = new PosixParser();
    	CommandLine cmd = null;
    	Boolean err = false;

    	try {
			cmd = parser.parse( options, args);
		} catch (ParseException e) {
			e.printStackTrace();
			System.exit(1);
		}
    	
    	if ( cmd.hasOption("u") ) {  
    		username = cmd.getOptionValue("u");
    	}
    	
    	if ( cmd.hasOption("t") ) {  
    		tenantName = cmd.getOptionValue("t");
        }
    	
    	if ( cmd.hasOption("p") ) {  
    		password = cmd.getOptionValue("p");
        }

    	if ( !cmd.hasOption("a") ) {  
    		System.err.println("Must specify -a or --api!");
    		err = true;
    	} else {
    		apiEndpoint = cmd.getOptionValue("a");
        }
    	
    	if (err)
    		System.exit(1);
    }
    
    private void run() {
    	init();
    	showAllUsers();
    	showAllTenants();
    	showAllInstances();
    	showAllHosts();
    	showAllHypervisors();
    }

    private void init() {
    	// Create the REST Client Connector, which also handles the JSON API serialization
    	connector = new JerseyConnector();
    	
    	// Create the Keystone Client Resource
		keystone = new Keystone(apiEndpoint, connector);
		UsernamePassword authentication = new UsernamePassword(username, password);

		// Authenticate with Keystone Admin API and get access credentials (especially token)
		access = keystone.tokens()
					.authenticate(authentication)
					.withTenantName(tenantName)
					.execute();

		// Set the Keystone Client Resource to use token when communicating with keystone
		keystone.setTokenProvider(new OpenStackSimpleTokenProvider(access.getToken().getId()));

		// List services
		System.out.println("Services:");
		Services services = keystone.services().list().execute();
		for (Service service : services) {
			System.out.println("  " + service.getId() + ", " + service.getName() + ", " + service.getDescription());
		}

		// List endpoints, which map to services by the service ID
		System.out.println("Endpoints:");
		Endpoints endpoints = keystone.endpoints().list().execute();
		for (Endpoint endpoint : endpoints) {
			System.out.println("  " + endpoint.getServiceId() + ", " + endpoint.getAdminURL() + ", " + endpoint.getPublicURL());
		}
    }

    /**
     * Get access to the default tenant, specified with -t or --tenant
     * @return Access
     */
    private Access getAccess() {
    	return access;
    }

    private Access getAccess(Tenant tenant) {
    	if (access.getToken().getTenant().getId() == tenant.getId()) {
    		return access;
    	}

    	// Use our existing token to authenticate with Keystone to create new tenant-specific tokens
		Authentication tokenAuth = new TokenAuthentication(access.getToken().getId());

		// Now, *try* to create a NEW token on this tenant.
		// NOTE: even "admin" users are not authorized on every tenant, though
		//    oddly they are authorized to add themselves to tenants!?
		try {
			return keystone.tokens()
					.authenticate(tokenAuth)
					.withTenantId(tenant.getId())
					.execute();
		} catch (OpenStackResponseException err) {
			System.out.println("Failed to access tenant " + tenant.getName());
			return null;  // Cannot access nova
		}
	}
    
    /**
	 * Create a Nova Client Resource using the default Keystone Admin tenant
	 * @return Nova
	 */
	private Nova createNovaClient() {
		Tenant tenant = access.getToken().getTenant();
		return createNovaClient(tenant);
	}
	
	/**
	 * Create a Nova Client Resource by authenticating with Keystone to gain
	 * access to the specified tenant.
	 * 
	 * Note: if the user does not have access to the tenant, will return null
	 * 
	 * @param Tenant who we are binding the Nova to
	 * @return Nova
	 */
	private Nova createNovaClient(Tenant tenant) {
		Access access = getAccess(tenant);
		if (access == null) {
			return null;	// Our user may not have access to this tenant
		}
		String region = null; // Don't care which region
		String facing = "admin"; // "admin", "public", or "internal"
		
		// Find the appropriate Nova endpoint from the access credentials
		// NOTE: the Nova endpoint is specific to the tenant used to authenticate - cannot see compute resources from other tenants!!!
		String novaApiEndpoint = KeystoneUtils.findEndpointURL(access.getServiceCatalog(), "compute", region, facing);
		Nova nova = new Nova(novaApiEndpoint, connector);
		nova.setTokenProvider(new OpenStackSimpleTokenProvider(access.getToken().getId()));		

		return nova;
	}
	
	private void showAllUsers() {
		System.out.println("\nUsers...\n");
		Users users = keystone.users().list().execute();
		for (User user: users) {
			System.out.println("  " + user.getId() + ", " + user.getName() + ", " + user.getUsername() + ", " + user.getEmail());
		}
	}
	
	private void showAllTenants() {
		System.out.println("\nTenants:");
		Tenants tenants = keystone.tenants().list().execute();
		for (Tenant tenant : tenants) {
			System.out.println("  " + tenant.getId() + ", " + tenant.getName() + ", " + tenant.getDescription());
		}
	}

	private void showAllInstances() {
		Boolean detail = true; // Access detailed view of Compute resources

		Nova nova = createNovaClient();

		System.out.println("\nMy Own Tenant's Instances:");
		Servers instances = nova.servers().list(detail).execute();
		for (Server instance : instances) {
			System.out.println("  " + instance.getId() + ", " + instance.getName() + ", " + instance.getHost());
		}

		Tenants tenants = keystone.tenants().list().execute();
		for (Tenant tenant : tenants) {
			nova = createNovaClient(tenant);
			if (nova == null) continue;
			
			System.out.println("\nInstances for tenant " + tenant.getName() + ":");
			instances = nova.servers().list(detail).execute();
			for (Server instance : instances) {
				System.out.println("  " + instance.getId() + ", " + instance.getName() + ", " + instance.getHost());
			}
		}
	}

	private void showAllHosts() {
		System.out.println("\nHosts:");
		HostsExtension hostsResource = new HostsExtension(createNovaClient());
		Hosts hosts = hostsResource.list().execute();
		for (Host host : hosts) {
			System.out.println("  " + host.getHostName() + ", " + host.getService());
		}
	}

	private void showAllHypervisors() {
		System.out.println("\nHypervisors:");
		Nova nova = createNovaClient();
		
		// No Hypervisors resource, so make an ad-hoc one
		OpenStackRequest<Hosts> hypervisorsList = new OpenStackRequest<Hosts>(nova, HttpMethod.GET, "/os-hypervisors/detail", null, Hosts.class);
		
		hypervisorsList.execute();
	}
}
