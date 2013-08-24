java-openstack-sdk-cli-example
==============================

Example app to help people get started using the
[java-openstack-sdk](https://github.com/https://github.com/woorea/openstack-java-sdk) Java binding
for the OpenStack APIs.

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
    http://192.168.27.100:35357/v2.0/

You can also run `mvn exec:java` to run the app.  The default config settings will work with
[DevstackUp](https://github.com/jogo/DevstackUp).

## Author

[Jeff Dutton](https://github.com/jdutton)


## Contributing

This is just for learning and to demo the API.  If you find errors or want to extend the example to
help other people learn, pull requests are welcome.
