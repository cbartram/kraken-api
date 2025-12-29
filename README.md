<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://kraken-plugins.com">
    <img src="lib/src/main/resources/kraken.png" alt="Logo" width="128" height="128">
  </a>

<h3 align="center">Kraken API</h3>

  <p align="center">
   An extended RuneLite API for creating plugins that support client interactions.
    <br />
</div>

[![Release Kraken API](https://github.com/cbartram/kraken-api/actions/workflows/release.yml/badge.svg?branch=master)](https://github.com/cbartram/kraken-api/actions/workflows/release.yml)
[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]

---

# Getting Started

Kraken API is designed to extend the RuneLite API with additional client interaction utilities for writing automation-based plugins that are fully compatible with RuneLite.
This API uses network packets to perform "click" interactions within the game client and is based on mappings defined by the [EthanVann API](https://github.com/Septharoth/EthanVannPlugins/tree/master). It's also worth shouting
out the [Vitalite](https://github.com/Tonic-Box/VitaLite/) client and project as their open source expertise of the game client helped make some of the Kraken API possible!
Specifically credit to Vitalite's Dialogue API and inspiration on Kraken's `TaskChain` and `ReplayStrategy` for mouse movement!

## API Usage

The following RuneLite "plugin" is purely for an example of the API's capabilities and isn't a full-fledged automation script.

```java
@PluginDescriptor(
        name = "Example",
        description = "Example plugin"
)
public class ExamplePlugin extends Plugin {
    
    @Inject
    private Context ctx;
    
    @Inject
    private BankService bank;
    
    @Inject
    private MovementService movement;
    
    @Inject
    private PrayerService prayer;
    
    @Subscribe
    private void onGameTick(GameTick e) {
      Player local = ctx.players().local().raw();
      
      if(local.isInteracting()) {
          return;
      }
      
      if(!bank.isOpen()) {
          // Open a bank
          ctx.gameObjects().withName("Bank booth").nearest().interact("Open");
      } else {
          // Withdraw a Rune Scimitar
          ctx.bank().nameContains("Rune scimitar").first().withdraw();
      }
      
      // Wield a Rune Scimitar
      ctx.equipment().withId(1333).first().wield();
      
      // Move to a new position
      movement.moveTo(new WorldPoint(1234, 5678));
      
      // Activate a protection prayer
      prayer.activatePrayer(Prayer.PROTECT_FROM_MELEE);
      
      // "Click" on a Goblin and attack it.
      ctx.npcs().withName("Goblin")
            .except(n -> n.raw().isInteracting())
            .nearest()
            .interact("Attack");
      
      
      // Take the goblin bones
      ctx.groundItems().reachable()
              .within(5)
              .filter(item -> item.name().equalIgnoreCase("bones"))
              .first()
              .take();
      
      // Bury the bones
      ctx.inventory().withName("Bones").first().interact("Bury");
    }
}
```

To use the API in an actual RuneLite plugin, you should check out the [Kraken Example Plugin](https://github.com/cbartram/kraken-example-plugin)
which shows a best practice usage of the API within an actual plugin.
To set up your development environment, we recommend following [this guide on RuneLite's Wiki](https://github.com/runelite/runelite/wiki/Building-with-IntelliJ-IDEA).

Once you have the example plugin cloned and setup within Intellij, you can run the main class in `src/test/java/ExamplePluginTest.java` to run RuneLite with
the example plugin loaded in the plugin panel within RuneLite's sidebar. See [consuming the API](#consuming-the-api) section for more information on
integrating the API into your plugins and build process.

![example-plugin](./images/example-plugin.png)

> If you are just looking to use pre-existing plugins, you can skip this repository and head over to our website: [kraken-plugins.com](https://kraken-plugins.com). 
> For more documentation on the API and Kraken plugins please see our [official documentation here](https://kraken-plugins.com/docs/).

### Prerequisites
- [Java 11+](https://adoptium.net/) (JDK required)
- [Gradle](https://gradle.org/) (wrapper included, no need to install globally)
- [Git](https://git-scm.com/)
- [RuneLite](https://runelite.net) (for testing and running plugins)

### Building

You can build the project with Gradle:

```bash
./gradlew clean build
```

The output API `.jar` will be located in:

```
./lib/build/libs/kraken-api-<version>.jar
```

## Gradle Example (Simple)

Although we recommend using the [Github packages approach](#gradle-example-recommended) to access the API since it is more reliable, [Jitpack](https://jitpack.io/) can get you set up with
the Kraken API without a personal access token.

```groovy
plugins {
    id 'java'
    id 'application'
}

// Replace with the package version of the API you need
def krakenApiVersion = 'X.Y.Z'

allprojects {
    apply plugin: 'java'
    repositories {
        maven { url 'https://jitpack.io' }
    }
}


dependencies {
    compileOnly group: 'com.github.cbartram', name:'kraken-api', version: krakenApiVersion
    // ... other dependencies
}
```

## Gradle Example (Recommended)

To use the API jar file in your plugin project you will need to either:
- `export GITHUB_ACTOR=<YOUR_GITHUB_USERNAME>; export GITHUB_TOKEN=<GITHUB_PAT`
- or add the following to your `gradle.properties` file: `gpr.user=your-github-username gpr.key=your-personal-access-token`

More information on generating a GitHub Personal Access token can [be found below](#authentication).

###  Authentication

Since the API packages are hosted on [GitHub Packages](https://docs.github.com/en/packages/learn-github-packages/introduction-to-github-packages) you will
need to generate a [Personal Access Token (PAT)](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens?versionId=free-pro-team%40latest&productId=packages&restPage=learn-github-packages%2Cintroduction-to-github-packages) on GitHub
to authenticate and pull down the API.

You can generate a GitHub PAT by navigating to your [GitHub Settings](https://github.com/settings/personal-access-tokens)
and clicking "Generate new Token." Give the token a unique name and optional description with read-only access to public repositories. Store the token
in a safe place as it won't be viewable again. It can be used to authenticate to GitHub and pull Kraken API packages.

> :warning: Do **NOT** share this token with anyone.

```groovy
plugins {
    id 'java'
    id 'application'
}


// Replace with the package version of the API you need
def krakenApiVersion = 'X.Y.Z'

allprojects {
    apply plugin: 'java'
    repositories {
        // You must declare this maven repository to be able to search and pull Kraken API packages
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/cbartram/kraken-api")
            credentials {
                username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
        }

        // Jitpack is an alternative means of accessing the API Jar file
        maven { url 'https://jitpack.io' }
    }
}


dependencies {
    compileOnly group: 'com.github.cbartram', name:'kraken-api', version: krakenApiVersion
    implementation group: 'com.github.cbartram', name:'shortest-path', version: '1.0.3'
    // ... other dependencies
}
```

### Packets & Reflection

When the API starts it will dynamically parse necessary client methods to determine which methods are used to send packets. These methods are then
invoked via reflection to actually send client packets to the OSRS servers. This enables the API to use network packets to communicate directly 
with OSRS servers and facilitate automatic interactions with the game client.

The core packet logic was originally written and used by the Packet Utils plugin [found here](https://github.com/Ethan-Vann/PacketUtils/blob/master/src/main/java/com/example/Packets/BufferMethods.java).
A good portion of the code has been re-written to follow best practices (using logs, factory design pattern, removing redundant code, refactoring to an API instead of plugin, etc...) however,
the functionality for client analysis, packet wrappers, and packet ops are sourced from the Packet Utils repository.
(Credit to EthanVann and contributors on the repo for mapping obfuscated class names and packet logic.)

### API Design & Methodology

The API is broken up into 2 distinct ways of accessing game information:

- Services (`com.kraken.api.service`)
- Query System (`com.kraken.api.query`)

Each API paradigm has its strengths, and it's likely you will need both when building semi and fully autonomous RuneLite
plugins. Read more about each API paradigm below to see which one (or a combination of both) suites your plugin needs.

#### Services

Services leverage the software design pattern of dependency injection. This is the exact same pattern adopted by RuneLite 
to ensure that plugins get exactly what they need to run from RuneLite and nothing more. As the developer you will declare to 
your script what you need from the Kraken API and the dependencies will be directly injected into your script at runtime.
Dependency injection ensures that your script classes remain lightweight, testable, and easy to debug.

The Service API paradigm is useful for static widgets or global game entities, for example:

- Bank interface - There is only a single bank interface to open, close, and set withdrawal modes on
- Prayers - A finite number of static prayer widgets
- Spells - A fixed number of in-game spells
- UI - Static utilities for calculating UI element bounds, interfacing with Dialogue, and switching client tabs
- Camera - A single camera exists and is centered around your local player (`ctx.cameras().first()` doesn't really make much sense!)
- etc...

If you needed to toggle a prayer, cast a spell, or close the bank then the service API paradigm would suite your plugin
well.

#### Query System

The query system allows you to flexibly "query", refine, and filter for dynamic game entities like:

- Players
- NPC's
- Game objects
- Ground Items
- Widgets
- Worn equipment (in the interface as well as your inventory)
- Inventory items
- and Bank items

The query paradigm wraps familiar RuneLite API objects with an `Interactable` interface allowing you to not
only __find__ game entities but also __interact__ with them in a straightforward fashion. 
All interactions use network packets to communicate directly with the game servers.

The API utilizes method chaining to filter for specific game entities loaded within the scene and exposes all methods on the underlying RuneLite 
API objects using the `raw()` method on every wrapped game entity class. 

The entire query API is exposed through a single class called the game `Context`.
This singleton class allows you to have one lightweight dependency which functions as a facade to query just about any game entity you would want for plugin development.

For example, to attack a nearby Goblin: 

```java
@PluginDescriptor(
        name = "Example",
        description = "Example plugin"
)
public class ExamplePlugin extends Plugin {
    
    @Inject
    private Context ctx;
    
    @Subscribe
    private void onGameTick(GameTick e) {
        Player local = ctx.players().local().raw();
        
        if(local.isInteracting()) {
            return;
        }

        ctx.npcs().withName("Goblin")
                .except(n -> n.raw().isInteracting())
                .nearest()
                .interact("Attack");
    }
}
```

The entire query API is designed to be thread safe so any queries, filters, or interactions can be run on non-client threads. 
When callable methods need to execute on RuneLite's client thread they will be scheduled there, blocking until the method executes. 
This helps ensure your plugin code is fully thread safe, predictable, and easy to read.

To see specific examples of various queries check out the [API tests](https://github.com/cbartram/kraken-api/tree/master/lib/src/test/java) which utilize a real RuneLite plugin to query and find
various game entities around Varrock east bank.

> :warning: When running on non-client threads the action must be scheduled on the client thread and is thus asynchronous in nature.

### Structure

The Kraken API exposes both high and low level functions for working with
game objects, NPC's, movement, pathing, simulations, network packets, and more. 
The documentation below describes the most likely packages developers will use when writing scripts or plugins.

- `core` - The package contains abstract base class logic which is used internally by different API methods and exposes the `Script` class.
  - `core.packet` - The package includes low and high level API's to send network packets to game servers for automation actions.
    - `core.packet.entity` - Generally most packet layer use cases will use the `core.packet.entity` API for interaction with Game Objects, NPC's, interfaces, and players.
- `service` - The package contains high level API's for directly interacting with static/global game elements such as (banking, prayer, spells, etc...) and use the `core.packet` package to form the backbone for the interactions
- `input` - Contains classes to help process and use input devices like mouses and keyboards.
- `query` - Contains the query API classes for finding and interacting with dynamic game elements like: inventory, npcs, players, game objects, and more.
- `overlay` - Contains simple and common overlays which can be directly used in RuneLite plugins e.g. Mouse position
- `sim` - Includes classes for simulating game ticks, NPC pathing, movement, line of sight, and players. This is useful for advanced
  plugins which evaluate hundreds of potential outcomes every game tick to determine the best "decision". e.g. Inferno and Colosseum plugins

### Scripting

For more information on writing scripts using the Kraken API 
check out the detailed [scripting guide](docs/SCRIPTING.md).

### Mouse Movement

For more information on mouse movement in the API check out the
detailed [mouse movement guide](docs/MOUSE.md)

### Game Updates

Game updates (especially new revisions) can quickly break a lot of the packet and interaction functionality in the API. 
Since the packet functionality is based on the [PacketUtils repository](https://github.com/Ethan-Vann/PacketUtils) this API
is constrained to the time it takes their developers to run their update and mapping process to generate a new `ObfuscatedNames.java`
file.

This file maps specific fields, methods, values, and classes from the obfuscated game client to be used in order to send packets and provide much of the API's functionality correctly.
The core packet logic was originally written and used by the Packet Utils plugin [found here](https://github.com/Ethan-Vann/PacketUtils/blob/master/src/main/java/com/example/Packets/BufferMethods.java).
A good portion of the code has been re-written to follow best practices (using logs, factory pattern, removing redundant code, refactoring to a library instead of plugin, etc...) however,
the functionality for client analysis, obfuscated class names, and packet ops are sourced from the Packet Utils repository (credit to EthanVann and contributors on the repo for mapping obfuscated class names and packet logic).

- Check the [PRs](https://github.com/Ethan-Vann/PacketUtils/pulls) for the Packet Utils repository. 
- Once the new `ObfuscatedNames` is updated copy the contents of the file into `core.packets.ObfuscatedNames` 
- Run a build to make sure everything compiles
- Run the example plugin to make sure packets still function correctly and the right packet class can be located for the RuneLite version and revision
- Commit and open a PR to build a new version of the API

## Running Tests

Please see the [testing guide](docs/TESTS.md) for more information on running tests.

## Development Workflow

1. Create a new branch from `master`
2. Implement or update your plugin/feature for the API
3. Add tests for new functionality
4. Run `./gradlew build` to verify that the API builds and tests pass
5. Commit your changes with a clear message `git commit -m "feat(api): Add feature X to Kraken API"`
6. Open a Pull Request

---

## Deployment

The Kraken API is automatically built and deployed via GitHub actions on every push to the `master` branch.
The latest version can be found in the [releases](https://github.com/cbartram/kraken-api/releases) section of the repository.

The deployment is fully automated and consists of:

-  Building the API JAR
- Publishing a new version to the GitHub Releases section
  - This will be picked up by Github Packages for easy integration into other gradle projects.
- Uploading the JAR file to the Minio storage server used by the Kraken Client at runtime.

---

## 🛠 Built With

* [Java](https://www.java.org/) — Core language
* [Gradle](https://gradle.org/) — Build tool
* [RuneLite](https://runelite.net) — Used for as the backbone for the API

---

## 🤝 Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

---

## 🔖 Versioning

We use [Semantic Versioning](http://semver.org/).
See the [tags on this repository](https://github.com/cbartram/kraken-api/tags) for available releases.

CI will automatically bump the patch version on each merge to master i.e. `1.1.4` -> `1.1.5`. If you want to bump 
a minor or major version then update the `version.txt` file in the root of the repository with the new version you
want to use as a base.

For example, moving from: `1.3.5` -> `1.4.0` the `version.txt` should be `1.4.0`.

---

## 📜 License

This project is licensed under the [GNU General Public License 3.0](LICENSE.md).

---

## 🙏 Acknowledgments

* **RuneLite** — For API's to work with and view in game data for Old School RuneScape
* **Packet Utils** - [Plugin](https://github.com/Ethan-Vann/PacketUtils) from Ethan Vann providing access to complex packet sending functionality which was used to develop the `core.packet` package of the API
* **Vitalite** - Vitalite for showing some incredible open source examples of dialogue, packets, mouse movement, and just working with the client in general
* **Microbot** — For clever ideas on client and plugin interaction using reflection.

[contributors-shield]: https://img.shields.io/github/contributors/cbartram/kraken-api.svg?style=for-the-badge
[contributors-url]: https://github.com/cbartram/kraken-api/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/cbartram/kraken-api.svg?style=for-the-badge
[forks-url]: https://github.com/cbartram/kraken-api/network/members
[stars-shield]: https://img.shields.io/github/stars/cbartram/kraken-api.svg?style=for-the-badge
[stars-url]: https://github.com/cbartram/kraken-api/stargazers
[issues-shield]: https://img.shields.io/github/issues/cbartram/kraken-api.svg?style=for-the-badge
[issues-url]: https://github.com/cbartram/kraken-api/issues

