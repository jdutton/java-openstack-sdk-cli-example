package example;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.cli.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import com.woorea.openstack.base.client.HttpMethod;
import com.woorea.openstack.base.client.OpenStackClientConnector;
import com.woorea.openstack.base.client.OpenStackRequest;
import com.woorea.openstack.base.client.OpenStackResponseException;
import com.woorea.openstack.base.client.OpenStackSimpleTokenProvider;
import com.woorea.openstack.glance.Glance;
import com.woorea.openstack.glance.model.Image;
import com.woorea.openstack.glance.model.Images;
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
import com.woorea.openstack.nova.model.Flavor;
import com.woorea.openstack.nova.model.Flavors;
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

	/**
	 * Which API service endpoints 
	 */
	private String facing = "public"; // "admin", "public", or "internal"

	
	/**
	 * Enable debug logging
	 */
	private boolean debug = false;

	// Client objects shared by multiple methods
	private OpenStackClientConnector connector;
	private Access access;
	private Keystone keystone;
	
	private ObjectMapper mapper = new ObjectMapper();

	
	/**
	 * 
	 * @param args
	 */
	public static void main( String[] args )
    {
		Cli app = new Cli();
    	app.parseOptions(args);

    	System.out.println("API Endpoint: " + app.apiEndpoint);
    	System.out.println("User:         " + app.username);
    	System.out.println("Tenant:       " + app.tenantName);
    	System.out.println("Password:     " + app.password);
    	System.out.println("API Facing:   " + app.facing);
    	System.out.println("Debug:        " + app.debug);

    	app.run();
    }
    
    private void parseOptions( String[] args ) {
    	Options options = new Options();
    	options.addOption("a", "api", true, "Keystone Admin API endpoint");
    	options.addOption("u", "user", true, "Username");
    	options.addOption("t", "tenant", true, "Tenant Name");
    	options.addOption("p", "password", true, "Password");
    	options.addOption("f", "facing", true, "API facing endpoints (\"admin\", \"internal\", or default of \"public\")");
    	options.addOption("d", "debug", false, "Enable debug logging");
    	
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
    	
    	if ( cmd.hasOption("f") ) {  
    		facing = cmd.getOptionValue("f");
        }

    	if ( cmd.hasOption("d")) {
    		debug = true;
    	}
    	
    	if (err)
    		System.exit(1);
    }
    
    private void run() {
    	init();
    	showAllTenants();
    	showAllUsers();
    	showAllFlavors();
    	showAllImages();
    	showAllHosts();
    	showAllHypervisors();
    	showAllInstances();
    	
    	System.out.println("Success :-)");
    }
    
    private void printJson(Object obj) {
		try {
			ObjectWriter writer = mapper.writer().withDefaultPrettyPrinter();
		    System.out.println(writer.writeValueAsString(obj));			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }

    private void init() {
    	// Create the REST Client Connector, which also handles the JSON API serialization
    	Logger logger = null;
    	if (debug)
    		logger = Logger.getLogger("os");
    	connector = new JerseyConnector(logger);
    	
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
		System.out.println("\n\nServices:");
		Services services = keystone.services().list().execute();
		for (Service service : services) {
			System.out.println("  " + service.getId() + ", " + service.getName() + ", " + service.getDescription());
		}

		// List endpoints, which map to services by the service ID
		System.out.println("\n\nEndpoints:");
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
    	if (getAccess().getToken().getTenant().getId() == tenant.getId()) {
    		return getAccess();
    	}

    	// Use our existing token to authenticate with Keystone to create new tenant-specific tokens
		Authentication tokenAuth = new TokenAuthentication(getAccess().getToken().getId());

		// Now, *try* to create a NEW token on this tenant.
		// NOTE: even "admin" users are not authorized on every tenant, though
		//    oddly they are authorized to add themselves to tenants!?
		try {
			return keystone.tokens()
					.authenticate(tokenAuth)
					.withTenantId(tenant.getId())
					.execute();
		} catch (OpenStackResponseException err) {
			System.out.println("\nFailed to access tenant \"" + tenant.getName() + "\" - user must not have authorization for this tenant!");
			return null;  // Cannot access nova
		}
	}
    
    /**
	 * Create a Nova Client Resource using the default Keystone Admin tenant
	 * @return Nova
	 */
	private Nova createNovaClient() {
		Tenant tenant = getAccess().getToken().getTenant();
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
		
		// Find the appropriate Nova endpoint from the access credentials
		// NOTE: the Nova endpoint is specific to the tenant used to authenticate - cannot see compute resources from other tenants!!!
		String apiEndpoint = KeystoneUtils.findEndpointURL(access.getServiceCatalog(), "compute", region, facing);
		Nova nova = new Nova(apiEndpoint, connector);
		nova.setTokenProvider(new OpenStackSimpleTokenProvider(access.getToken().getId()));		

		return nova;
	}
	
	/**
	 * Create a Glance Client Resource by asking Keystone
	 * 
	 * @return Glance
	 */
	private Glance createGlanceClient() {
		Access access = getAccess();
		String region = null; // Don't care which region
		
		// Find the appropriate Glance endpoint from the access credentials
		String apiEndpoint = KeystoneUtils.findEndpointURL(access.getServiceCatalog(), "image", region, facing);

		if (!apiEndpoint.endsWith("/v1") && !apiEndpoint.endsWith("/v1/") &&
				!apiEndpoint.endsWith("/v2") && !apiEndpoint.endsWith("/v2/")) {
			// FIXME: Why do I need to explicitly add an API version!?
			// Without this a 300 Multiple Choices error is returned
			apiEndpoint += "/v1";
		}
		Glance glance = new Glance(apiEndpoint, connector);
		glance.setTokenProvider(new OpenStackSimpleTokenProvider(access.getToken().getId()));		

		return glance;
	}
	
	private void showAllTenants() {
		System.out.println("\n\nTenants:");
		Tenants tenants = keystone.tenants().list().execute();
		for (Tenant tenant : tenants) {
			System.out.println("  " + tenant.getId() + ", " + tenant.getName() + ", " + tenant.getDescription() + ", " + (tenant.getEnabled() ? "ENABLED" : "DISABLED"));
		}
	}

	private void showAllUsers() {
		System.out.println("\n\nUsers:\n");
		Users users = keystone.users().list().execute();
		for (User user: users) {
			printJson(user);
		}
	}
	
	private void showAllFlavors() {
		System.out.println("\n\nFlavors:\n");
		Nova nova = createNovaClient();
		Flavors flavors = nova.flavors().list(true/*detail*/).execute();
		for (Flavor flavor : flavors) {
			printJson(flavor);
		}
	}
	
	private void showAllImages() {
		System.out.println("\n\nImages:\n");
		Glance glance = createGlanceClient();
		Images images = glance.images().list(true/*detail*/).execute();
		for (Image image : images) {
			printJson(image);
		}
	}

	private void showAllHosts() {
		System.out.println("\n\nHosts:");
		HostsExtension hostsResource = new HostsExtension(createNovaClient());
		Hosts hosts = hostsResource.list().execute();
		for (Host host : hosts) {
			printJson(host);
		}
	}

	private void showAllHypervisors() {
		System.out.println("\n\nHypervisors:");
		Nova nova = createNovaClient();
		
		// No Hypervisors resource, so make an ad-hoc one
		OpenStackRequest<Hypervisors> hypervisorsList = new OpenStackRequest<Hypervisors>(nova, HttpMethod.GET, "/os-hypervisors/detail", null, Hypervisors.class);		
		Hypervisors hypervisors = hypervisorsList.execute();
		for (Hypervisor hypervisor : hypervisors) {
			printJson(hypervisor);
		}
	}

	private void showAllInstances() {
		Boolean detail = true; // Access detailed view of Compute resources

		Nova nova = createNovaClient();

		System.out.println("\n\nLogin Token's Tenant's Instances (\"" + tenantName + "\"):");
		Servers instances = nova.servers().list(detail).execute();
		for (Server instance : instances) {
			System.out.println("  " + instance.getId() + ", " + instance.getName() + ", " + instance.getHost());
		}

		Tenants tenants = keystone.tenants().list().execute();
		for (Tenant tenant : tenants) {
			nova = createNovaClient(tenant);
			if (nova == null) continue;

			System.out.println("\n\nInstances for tenant \"" + tenant.getName() + "\":");
			instances = nova.servers().list(detail).execute();
			for (Server instance : instances) {
				printJson(instance);
			}
		}
	}
}
