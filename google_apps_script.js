function doPost(e) {
    var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
    var data = JSON.parse(e.postData.contents);
    sheet.appendRow([
        new Date().toISOString(),
        data.sender || "",
        data.userName || "",
        data.encryptedMessage || ""
    ]);
    return ContentService.createTextOutput(JSON.stringify({ status: "ok" }))
        .setMimeType(ContentService.MimeType.JSON);
}

function doGet(e) {
    var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
    var lastRow = sheet.getLastRow();
    if (lastRow < 1) {
        return ContentService.createTextOutput(JSON.stringify({ status: "empty" }))
            .setMimeType(ContentService.MimeType.JSON);
    }
    var row = sheet.getRange(lastRow, 1, 1, 4).getValues()[0];
    var result = {
        status: "ok",
        timestamp: row[0],
        sender: row[1],
        userName: row[2],
        encryptedMessage: row[3]
    };
    return ContentService.createTextOutput(JSON.stringify(result))
        .setMimeType(ContentService.MimeType.JSON);
}
