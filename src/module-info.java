module FoundationsF26 {
	requires javafx.controls;
	requires java.sql;
	requires java.net.http;
	
	opens applicationMain to javafx.graphics, javafx.fxml;
}
