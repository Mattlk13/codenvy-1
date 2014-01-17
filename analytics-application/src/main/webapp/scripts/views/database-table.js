if (typeof analytics === "undefined") {
    analytics = {};
}

analytics.views = analytics.views || {};
analytics.views.databaseTable = new DatabaseTable();

function DatabaseTable() {  
   var lastClickedRow;
   var lastMouseoveredRow;
   
   // ***** Private members    
   var MOUSEOVER_ROW_STYLE = " mouseover-row";
   var CLICKED_ROW_STYLE = " clicked-row";
 
   var setupRowHandlers = function(ignoreLastRow) {
      var tables = document.getElementsByClassName("database-table");
      if (tables != null) {      
         for(var i = 0; i < tables.length; i++) {
            var table = tables[i];
            var rows = table.rows;
            
            // don't take into account header and footer rows
            var rowCount = rows.length;
            if (typeof ignoreLastRow == "boolean" && ignoreLastRow == true) {
               rowCount--;
            }            

            for (var j = 1; j < rowCount; j++) {
               var row = rows[j];
               
               // add click event handler
               row.addEventListener("click", onRowClick, true);
               
               // add mouse mouseover event handler
               row.addEventListener("mouseover", onRowMouseover, true);
               
               // add mouse mouseout event handler
               row.addEventListener("mouseout", onRowMouseout, true);
            }
         }
      }
   };
   
   var setupVerticalRowHandlers = function() {
      var tables = document.getElementsByClassName("database-table-vertical-row");
      if (tables != null) {      
         for(var i = 0; i < tables.length; i++) {
            var table = tables[i];
            var rows = table.rows;
            
            for (var j = 0; j < rows.length; j++) {
               var row = rows[j];
               
               // add click event handler
               row.addEventListener("click", onRowClick, true);
               
               // add mouse mouseover event handler
               row.addEventListener("mouseover", onRowMouseover, true);
               
               // add mouse mouseout event handler
               row.addEventListener("mouseout", onRowMouseout, true);
            }
         }
      }
   };
   

    function onRowClick(event) {
        var row = event.currentTarget;

        if (typeof lastClickedRow != "undefined") {
            lastClickedRow.className = lastClickedRow.className.replace(
                    CLICKED_ROW_STYLE, "");
        }

        if (typeof lastMouseoveredRow != "undefined") {
            lastMouseoveredRow.className = lastMouseoveredRow.className
                    .replace(MOUSEOVER_ROW_STYLE, "");
        }

        row.className += CLICKED_ROW_STYLE;

        lastClickedRow = row;
    }
   
   function onRowMouseover(event) {	
      var row = event.currentTarget;
      if (row != lastClickedRow) {
         row.className += MOUSEOVER_ROW_STYLE;
      }
      
      lastMouseoveredRow = row;
   }
   
   function onRowMouseout(event) {
      if (typeof lastMouseoveredRow != "undefined") {
         lastMouseoveredRow.className = lastMouseoveredRow.className.replace(MOUSEOVER_ROW_STYLE, "");       
      }
   }

	
   /** ****************** API ********** */
	return {
		setupVerticalRowHandlers: setupVerticalRowHandlers,
		setupRowHandlers: setupRowHandlers
	}
}