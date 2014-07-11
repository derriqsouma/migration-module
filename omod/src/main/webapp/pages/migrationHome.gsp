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
        </ul>
        <div id="tabs-1">

            <form action="migrationHome.page" method="post">
                <input type="file" name="file" id="fileUpload" size="50"/>
                <br/><br/>
                <div class="ke-form-footer">
                    <button type="submit"><img src="${ ui.resourceLink("kenyaui", "images/glyphs/ok.png") }" />Submit</button>
                </div>
            </form>
            ${file}

        </div>
        <div id="tabs-2">
            ${ ui.includeFragment("migrate", "moh361B")}

        </div>
        <div id="tabs-3">

        </div>
    </div>
</div>

<script>

    jQuery(function() {
        jQuery( "#tabs" ).tabs();

    });
</script>
