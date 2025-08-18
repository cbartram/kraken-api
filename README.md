<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://kraken-plugins.duckdns.org">
    <img src="lib/src/main/resources/kraken.png" alt="Logo" width="128" height="128">
  </a>

<h3 align="center">Kraken API</h3>

  <p align="center">
   An extended RuneLite API for creating Kraken Plugins that support client interactions.
    <br />
</div>

[![Release Kraken API](https://github.com/cbartram/kraken-api/actions/workflows/release.yml/badge.svg?branch=master)](https://github.com/cbartram/kraken-api/actions/workflows/release.yml)
[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]

---

# 🚀 Getting Started (For Developers)

Kraken API is designed to extend the RuneLite API with additional client-interaction utilities for writing automation based plugins that are compatible with RuneLite. If you are
just looking to use pre-existing plugins, you can skip this repository and head over to our website: [kraken-plugins.duckdns.org](https://kraken-plugins.duckdns.org).

### Prerequisites
- [Java 11+](https://adoptium.net/) (JDK required)
- [Gradle](https://gradle.org/) (wrapper included, no need to install globally)
- [Git](https://git-scm.com/)
- [RuneLite](https://runelite.net) (for testing and running plugins)


### Cloning the Repository
```bash
git clone https://github.com/cbartram/kraken-api.git
cd kraken-api
````

### Building

You can build the project with Gradle:

```bash
./gradlew clean build
```

The output API `.jar` will be located in:

```
./lib/build/libs/kraken-api-<version>.jar
```

### Running Tests

Run the full test suite with:

```bash
./gradlew test
```

### Using Kraken API in Your Plugin

Add the dependency to your `build.gradle`:

```gradle
repositories {
    mavenCentral()
    maven { url "https://maven.pkg.github.com/cbartram/kraken-api" }
}

dependencies {
    implementation group: 'com.github.cbartram', name:'kraken-api', version: '1.0.12'
}
```

> ⚠️ You may need a GitHub token with `read:packages` permission to authenticate with the package registry.

### Development Workflow

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

A deployment consists of:

- Building the API JAR
- Publishing a new version to the GitHub Releases section
  - This will be picked up by jitpack.io for easy integration into other gradle projects.
- Uploading the JAR file to the Minio storage server used by the Kraken Client at runtime.
- (Optional) Updating the `bootstrap.json` in the Kraken Client to point to the latest version of the API JAR file
- (Optional) Updating the build.gradle file in the Kraken Client to use the latest version of the API JAR file

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

---

## 📜 License

This project is licensed under the [GNU General Public License 3.0](LICENSE.md).

---

## 🙏 Acknowledgments

* **RuneLite** — The splash screen and much of the core codebase come from RuneLite.
* **Microbot** — For clever ideas on client and plugin interaction.

[contributors-shield]: https://img.shields.io/github/contributors/cbartram/kraken-api.svg?style=for-the-badge
[contributors-url]: https://github.com/cbartram/kraken-api/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/cbartram/kraken-api.svg?style=for-the-badge
[forks-url]: https://github.com/cbartram/kraken-api/network/members
[stars-shield]: https://img.shields.io/github/stars/cbartram/kraken-api.svg?style=for-the-badge
[stars-url]: https://github.com/cbartram/kraken-api/stargazers
[issues-shield]: https://img.shields.io/github/issues/cbartram/kraken-api.svg?style=for-the-badge
[issues-url]: https://github.com/cbartram/kraken-api/issues

