java-openstack-sdk-cli-example
==============================

Example app to help people get started using the
[openstack-java-sdk](https://github.com/https://github.com/woorea/openstack-java-sdk) Java binding
for the OpenStack APIs.

All it does is probe an OpenStack using admin user credentials and a pointer to the Keystone API,
then print out a bunch of OpenStack objects and fields.  Note that the default keystone port 5000
doesn't allow many admin functions, so you will want to point to port 35357 (the Keystone Admin API
port).

I recommend running the app against a local [DevStack](http://devstack.org) instance of OpenStack where you can play and do
no harm (though the app currently only reads from the API).

If you are a [Vagrant] user like me, I recommend
[DevstackUp](https://github.com/jogo/DevstackUp) to create a local DevStack.

# Build

Use `mvn` to build the executable jar:

    mvn package

# Run

Run the executable jar, specifying the keystone user, tenant, password, and API endpoint, for
example:

    java -jar target/java-openstack-sdk-cli-example-*-with-dependencies.jar -a
    http://192.168.27.100:35357/v2.0/ -p adminpasswd --debug

Arguments include:

 * `-a` or `--api` for Keystone Admin API endpoint (e.g. `http://192.168.27.100:35357/v2.0/`)
 * `-u` or `--user` for Keystone username with admin privileges (defaults to `admin`)
 * `-t` or `--tenant` for the Tenant name that the user is logging into (defaults to `admin`)
 * `-p` or `--password` for the user's password (defaults to `password`)
 * `-f` or `--facing` to set the API facing endpoints (`admin`, `internal`, or default `public`)
 * `-d` or `--debug` to enable debug logging of HTTP client requests to the API (pretty verbose, but
   helpful to debug and get insight into API calls)
 
For example:
    java -jar target/java-openstack-sdk-cli-example-*-with-dependencies.jar -a
    http://10.11.12.13:35357/v2.0/ -u myuser -t mytenant -p mypassword

You can also run `mvn exec:java` to run the app.  The default config settings will work with
[DevstackUp](https://github.com/jogo/DevstackUp).

## Author

[Jeff Dutton](https://github.com/jdutton)


## Contributing

This is just for learning and to demo the API.  If you find errors or want to extend the example to
help other people learn, pull requests are welcome.
