<%
    ui.decorateWith("kenyaemr", "standardPage")
%>

<div class="ke-page-sidebar">

    <div class="ke-panel-frame">
        <div class="ke-panel-heading">Help</div>

        <div class="ke-panel-content">
            to be updated
        </div>
    </div>
</div>

<div class="ke-page-content">

    <div id="tabs" class="ke-tabs">
        <ul>
            <li><a href="#tabs-1">MOH_361A</a></li>
            <li><a href="#tabs-2">MOH_361B</a></li>
            <li><a href="#tabs-3">MOH_408</a></li>
            <li><a href="#tabs-4">fragment</a></li>
        </ul>
        <div id="tabs-1">

            <form action="migrationHome.page" method="post">
                <input type="file" name="moh361Afile" id="moh361A" size="50"/>
                <br/><br/>
                <div class="ke-form-footer">
                    <button type="submit"><img src="${ ui.resourceLink("kenyaui", "images/glyphs/ok.png") }" />Submit</button>
                </div>
            </form>

        </div>
        <div id="tabs-2">

            <form action="migrationHome.page" method="post">
                <input type="file" name="moh361Bfile" id="moh361B" size="50"/>
                <br/><br/>
                <div class="ke-form-footer">
                    <button type="submit"><img src="${ ui.resourceLink("kenyaui", "images/glyphs/ok.png") }" />Submit</button>
                </div>
            </form>

        </div>
        <div id="tabs-3">
            <form action="migrationHome.page" method="post">
                <input type="file" name="moh408file" id="moh408" size="50"/>
                <br/><br/>
                <div class="ke-form-footer">
                    <button type="submit"><img src="${ ui.resourceLink("kenyaui", "images/glyphs/ok.png") }" />Submit</button>
                </div>
            </form>

        </div>

        <div id="tabs-4">
            ${ ui.includeFragment("migrate", "moh361B") }
        </div>
    </div>
</div>
<script>
    jQuery(function() {
        var index = 'key';
        var dataStore = window.sessionStorage;
        try{
            var oldIndex = dataStore.getItem(index);
        }catch(e){
            var oldIndex = 0;
        }

        jQuery( "#tabs " ).tabs({
//            event: "mouseover",
            active: oldIndex,
            activate: function(event, ui){
                var newIndex = ui.newTab.parent().children().index(ui.newTab);
                dataStore.setItem(index, newIndex)
            }
        });

    });
</script>
