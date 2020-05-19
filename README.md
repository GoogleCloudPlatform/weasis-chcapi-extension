## About Weasis Google DICOM Plugin

Plugin enables Weasis Viewer users access to [Google Cloud Healthcare API](https://cloud.google.com/healthcare) DICOM data.
It utilizes [DICOMweb REST API](https://cloud.google.com/healthcare/docs/how-tos/dicomweb) to interact with Google Cloud services.

### Features

* Login using your Google account
* Interactive exploration of Google Healthcare API Dicom stores
* Download and display all kinds of DICOM data
* Advanced study search capabilities

![Google Dicom Explorer](google_dicom_explorer.png)

### Running the plugin

The plugin runs as an extension to the main Weasis application, so first you
need to download the main Weasis application from https://nroduit.github.io/en/.

***Please note, latest supported release of Weasis is [3.0.4](https://github.com/nroduit/Weasis/releases/tag/3.0.4)***

Then you need to have existing data in the Cloud Healthcare API and install the
plugin to get up and running. Please see more detailed instructions below.

#### Setting up Google Cloud Healthcare API:

See https://cloud.google.com/healthcare/docs/ to get started.

#### Installing plugin

* Get the latest release JAR from this repositories releases tab.
* Follow instructions at [installing
  plug-ins](https://nroduit.github.io/en/basics/customize/build-plugins/#install-plug-ins)
  to add this plugin to Weasis.
* Copy your client_secrets.json to weasis-portable root folder (or configure the path of the json file with the property "google.client.secret" in conf/ext-config.properties
  Example for the web distribution: google.client.secret={felix.cache.rootdir}/.weasis/client_secrets.json
* Run Weasis Viewer executable
* Switch to **_Google Dicom Explorer_** tab and login using your Google Account

NOTE: If you're running on Linux and find that Weasis is stuck loading plugins,
check the terminal output for a link to sign-in with your Google account.

### Building plugin

If you're just trying to run the tool, please see the instructions above. If you
need to recompile the plugin for any reason here are the steps to do so.

Weasis requires JDK8.
Plugin depends on core Weasis API, that's why you have to clone, build and install core Weasis modules to
your local Maven repository first
For this purpose follow instructions at [building Weasis](https://nroduit.github.io/en/getting-started/building-weasis/).
After Weasis artifacts installed to your local Maven repository plugin itself can be compiled
Detailed build instruction can be found at
[building Weasis plugins](https://nroduit.github.io/en/basics/customize/build-plugins/)
Clone this repository and execute following script:
```bash
cd google-healthcare-weasis-plugin

## build plugin
mvn clean install
```


