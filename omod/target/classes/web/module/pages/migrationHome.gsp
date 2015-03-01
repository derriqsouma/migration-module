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
          <!--  <li><a href="#tabs-4">Enroll Into Hiv Program</a></li>-->
            <li><a href="#tabs-5">KAKUMA</a></li>
            <li><a href="#tabs-6">MARAGUA</a></li>
            <li><a href="#tabs-7">KISII</a></li>
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

        <!--<div id="tabs-4">
            ${ ui.includeFragment("migrate", "enrollIntoHivProgram") }
        </div>-->

        <div id="tabs-5">
            <form action="migrationHome.page" method="post">
                <input type="file" name="kakuma_patients" size="50"/>
                <br/><br/>
                <div class="ke-form-footer">
                    <button type="submit"><img src="${ ui.resourceLink("kenyaui", "images/glyphs/ok.png") }" />Submit</button>
                </div>
            </form>
        <br><br>
        <form action="migrationHome.page" method="post">
            <input type="file" name="kakuma_visits" size="50"/>
            <br/><br/>
            <div class="ke-form-footer">
                <button type="submit"><img src="${ ui.resourceLink("kenyaui", "images/glyphs/ok.png") }" />Submit</button>
            </div>
        </form>
        <br><br>
        <form action="migrationHome.page" method="post">
            <input type="file" name="kakuma_eid" size="50"/>
            <br/><br/>
            <div class="ke-form-footer">
                <button type="submit"><img src="${ ui.resourceLink("kenyaui", "images/glyphs/ok.png") }" />Submit</button>
            </div>
        </form>

        </div>

        <div id="tabs-6">
            <form action="migrationHome.page" method="post">
                <input type="file" name="maragua_patients"  size="50"/>
                <br/><br/>
                <div class="ke-form-footer">
                    <button type="submit"><img src="${ ui.resourceLink("kenyaui", "images/glyphs/ok.png") }" />Submit</button>
                </div>
            </form>
            <br><br>
            <form action="migrationHome.page" method="post">
                <input type="file" name="maragua_visits" size="50"/>
                <br/><br/>
                <div class="ke-form-footer">
                    <button type="submit"><img src="${ ui.resourceLink("kenyaui", "images/glyphs/ok.png") }" />Submit</button>
                </div>
            </form>

        </div>

        <div id="tabs-7">
            <form action="migrationHome.page" method="post">
                <input type="file" name="kisii_patients" id="kisii_patients" size="50"/>
                <br/><br/>
                <div class="ke-form-footer">
                    <button type="submit"><img src="${ ui.resourceLink("kenyaui", "images/glyphs/ok.png") }" />Submit</button>
                </div>
            </form>
            <br><br>
            <form action="migrationHome.page" method="post">
                <input type="file" name="kisii_visits" id="kisii_visits" size="50"/>
                <br/><br/>
                <div class="ke-form-footer">
                    <button type="submit"><img src="${ ui.resourceLink("kenyaui", "images/glyphs/ok.png") }" />Submit</button>
                </div>
            </form>

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
