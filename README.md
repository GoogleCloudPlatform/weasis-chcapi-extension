### About Weasis Google DICOM Plugin
Plugin enables Weasis Viewer users access to [Google Cloud Healthcare API](https://cloud.google.com/healthcare) DICOM data.
It utilizes [DICOMweb REST API](https://cloud.google.com/healthcare/docs/how-tos/dicomweb) to interact with Google Cloud services.
#### Features
* Login using your Google account
* Interactive exploration of Google Healthcare API Dicom stores
* Download and display all kinds of DICOM data
* Advanced study search capabilities

![Google Dicom Explorer](google_dicom_explorer.png)

#### Setting up Google Cloud Healthcare API:
* See https://cloud.google.com/healthcare/docs/ to get started.

#### Building plugin
Weasis requires JDK8.
Plugin depends on core Weasis API, that's why you have to clone, build and install core Weasis modules to
your local Maven repository first.
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

#### Installing plugin
* To install plugin into Weasis follow instruction at [installing plug-ins](https://nroduit.github.io/en/basics/customize/build-plugins/#install-plug-ins)
section of Weasis documentation.
* Run Weasis Viewer executable
* Switch to **_Google Dicom Explorer_** tab and login using your Google Account
* Explore your DICOM data
