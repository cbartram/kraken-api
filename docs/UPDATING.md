# Game Updates

The following documentation details how to update the API in response to game updates and new client revisions.
Game updates can be broken down into two main categories (with relation to this API):

- RuneLite updates (~weekly)
- Client Revision updates (~bi-monthly)
    - Packet Updates
    - Reflection Updates

## RuneLite Updates
RuneLite updates are often simple as the RuneLite API doesn't generally drastically change from release
to release. To test the API against the latest RuneLite version:

- Update `def runeLiteVersion = 'x.y.z'` in `build.gradle` to the latest version
- Compile the API against the newest version with `./gradlew clean build`
- Verify that there are no compile time errors.

If there are compile time errors, then the RuneLite API changed, and you will have to dig a little deeper
into what changed, where it went, and how to restore functionality in the API. Luckily, this doesn't happen
all that often despite weekly RuneLite updates.

## Client Revision Updates
Client revision updates are generally more work. The gampack JAR file will be re-obfuscated, and new mappings
will need to be generated to call/update the right classes, fields, and methods in the client that RuneLite
does not directly expose. Luckily, client revision updates are less common and generally occur bi-monthly or quarterly.
There are two primary parts of the API that need to be updated when a new revision is released. You can read
about each below.

### Packets
Game updates (especially new revisions) can quickly break a lot of the packet and interaction functionality in the API.
Since the packet functionality is based on the [PacketUtils repository](https://github.com/Ethan-Vann/PacketUtils) this API
is constrained to the time it takes their developers to run their update and mapping process to generate a new `ObfuscatedNames.java`
file.

This file maps specific fields, methods, values, and classes from the obfuscated game client to be used to send packets and provide much of the API's functionality correctly.
The core packet logic was originally written and used by the Packet Utils plugin [found here](https://github.com/Ethan-Vann/PacketUtils/blob/master/src/main/java/com/example/Packets/BufferMethods.java).
A good portion of the code has been re-written to follow best practices (using logs, factory pattern, removing redundant code, refactoring to a library instead of plugin, etc...) however,
the functionality for client analysis, obfuscated class names, and packet ops are sourced from the Packet Utils repository. (Credit to EthanVann and contributors on the repo for mapping obfuscated class names and packet logic.)

- Check the [PRs](https://github.com/Ethan-Vann/PacketUtils/pulls) for the Packet Utils repository.
- Once the new `ObfuscatedNames` is updated copy the contents of the file into `core.packets.ObfuscatedNames`
- Update the `private static final int REQUIRED_CLIENT_REV = 123;` in `com.kraken.api.core.packet.PacketMethodLocator` to the new client revision.
- Run a build to make sure everything compiles
- Run the example plugin to make sure packets still function correctly and the right packet class can be located for the RuneLite version and revision
- Commit and open a PR to build and release a new version of the API

### Reflection
Reflection hooks are used in several places within the API (like the `LoginService`) in order to map obfuscated game client
classes, methods, and fields to known functionality within the game client. 

For example when logging into the game client,
you must set the `loginIndex` field which is an integer determining the login flow for an account. To do this, 
we must maintain mappings between the obfuscated class and method names (`bz.aq` -> `Login.setLoginIndex()`) so that 
we can use reflection to invoke the right methods and set the right fields since RuneLite does not directly inject mixins
for the client functionality. 

These mappings are stored in the resources directory under `reflection_hooks.json`. This file must be updated for a new 
client revision (when methods and classes are re-obfuscated). To update these hooks:

- Go to the [Vitalite Mappings json](https://github.com/Tonic-Box/VitaLite/blob/main/src/main/resources/com/tonic/injector/mappings.json) file (huge shoutout for the VitaLite devs to maintain and publish these mappings)
- Check the following table for help finding the right mappings to update the hooks file
- Update the `reflection_hooks.json` stored in the resources directory with the new obfuscated field, class, and method names
- Open a PR and merge to publish the new reflection_hooks file to the server

| **Search Term**           | **JSON Mapping Key**    |
|---------------------------|-------------------------|
| `setLoginIndex`           | setLoginIndexMethodName |
| `setLoginIndex`           | setLoginIndexClassName  |
| `JX_SESSION_ID`           | jxSessionFieldName      |
| `JX_SESSION_ID`           | jxSessionClassName      |
| `JX_CHARACTER_ID`         | jxAccountIdFieldName    |
| `JX_CHARACTER_ID`         | jxAccountIdClassName    |
| `JX_DISPLAY_NAME`         | jxDisplayNameFieldName  |
| `JX_DISPLAY_NAME`         | jxDisplayNameClassName  |
| `accountTypeCheck`        | jxAccountCheckFieldName |
| `accountTypeCheck`        | jxAccountCheckClassName |
| `jagexType`               | jxJagexValueFieldName   |
| `jagexType`               | jxJagexValueClassName   |
| `legacyType`              | jxLegacyValueFieldName  |
| `legacyType`              | jxLegacyValueClassName  |
| `MouseHandler_idleCycles` | idleCyclesClassName     |
| `MouseHandler_idleCycles` | idleCyclesFieldName     |

#### Login Index Garbage Value

Finding this value is a little more complex. For the `setLoginIndexGarbageValue` you will need to get the `gamepack-{int}.jar` file by:

- Viewing the Jagex jav_config.ws at: https://oldschool.runescape.com/jav_config.ws
- Copy the `initial_jar=gamepack-{int}.jar` field's value (should be gamepack-123.jar for example)
- Copy the `codebase` value (should be something like http://oldschool188.runescape.com/)
- Combine them in the same URL to be something like: http://oldschool188.runescape.com/gamepack_5124771.jar

Next, add this gamepack jar file to your IDE as an additional library so you can view the classes. Use a decompiler to decompile the obfuscated bytecode
(most IDE's have this built in when opening up a compiled bytecode file).

- Find the `setLoginIndex` class name mapped using Vitalite's mapping from the table above in the jar file
  - i.e this will be something like `bz` or similar
- Find a `static void aq(int var0, int var1)` method in the bytecode with the signature accepting two integer parameters.

The method may not be named `aq` but its signature should match, and it will look something like:

```java
static void aq(int var0, int var1) {
    try {
        if (-1614560929 * bv.cv == var0) {
            if (var1 != 1845767532) {
                ;
            }
        } else {
            bv.cv = var0 * 625007263;
        }
    } catch (RuntimeException var2) {
        throw ab.at(var2, "bz.aq(" + ')');
    }
}
```

Your garbage value will be the value being compared to `var1` i.e `1845767532` in the example above.


