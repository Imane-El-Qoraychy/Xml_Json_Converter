# XML ⇄ JSON Converter

This project demonstrates the conversion between **XML** and **JSON** using **three different approaches**:
- Manual conversion
- Jackson library
- Spring Boot REST API

The objective is to understand the **structural differences between XML and JSON** and how they are handled in practice.

---

## Project Structure
    xml-json-converter/
    │
    ├──── xmljsonconverter/
    │  └── src/main/java/com/mycompany/xmljsonconverter/
    │  ├── MainApp.java
    │  ├── MainController.java
    │  ├── ManualConverterService.java
    │  ├── LocalConverterService.java
    │  ├── ApiConverterService.java
    │  ├── SimpleJsonParser.java
    │
    │  └── src/main/java/resources
    │  ├── main_view.fxml
    │
    ├── xmljson-api/
    │ └── src/main/java/com/mycompany/xmljsonapi/
    │ ├── XmljsonApiApplication.java
    │ └── ConvertController.java
    │
    ├── data/
    │ ├── library.xml
    │ └── library.json
    │ └── test.json
    │
    └── README.md


---

## Conversion Modes

- **Manual**: conversion implemented without external libraries  
- **Jackson**: conversion using `ObjectMapper` and `XmlMapper`  
- **API**: conversion via a Spring Boot REST API using HTTP

All modes produce equivalent results, but the internal implementation is different.

---

## XML vs JSON

- XML supports attributes and text inside the same element  
- JSON uses objects, arrays, and key–value pairs  
- Repeated XML elements become JSON arrays  
- Attributes may be represented differently in JSON  

A root element is added during XML -> JSON conversion to preserve the hierarchy,
and reconstructed when converting back to XML.

---

## Testing

The application was tested in both directions:
- XML → JSON
- JSON → XML

The data and overall structure are preserved across all conversion modes.

The application allows:
- Loading XML or JSON files
- Writing XML or JSON manually
- Starting a new test, which resets and initializes the input and output fields
- Converting between formats
- Saving the conversion result


---

##  Demo Video

The demo video is available directly on GitHub.
️ **See the video in the _Releases_ section of this repository**
or in Drive : 
https://drive.google.com/file/d/1b8JMJEJjvm7hQ_i_uYV2dzMSiqBWklho/view?usp=sharing

---

## Technologies Used

- Java
- JavaFX
- Spring Boot
- Jackson
- Maven
- Git & GitHub

## Author

- Name: IMANE EL QORAYCHY
- LinkedIn: https://www.linkedin.com/in/imane-el-qoraychy-011897368
