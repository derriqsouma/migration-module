<%

    ui.decorateWith("kenyaemr", "standardPage", [layout: "sidebar"])

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
    <div class="ke-tabmenu">

        <div class="ke-tabmenu-item" data-tabid="MOH 361A"></div>
        <div class="ke-tabmenu-item" data-tabid="MOH 361B"></div>

    </div>

    <div class="ke-tab" data-tabid="">
        <table cellspacing="0" cellpadding="0" width="100%">
            <tr>
                <td style="width: 50%; vertical-align: top">
                    <div class="ke-panel-frame">
                        <div class="ke-panel-heading">upload a file</div>
                        <div class="ke-panel-content">


                        </div>
                    </div>
                </td>

            </tr>
        </table>
    </div>

    <div class="ke-panel-content">

        <form action="migrationHome.page" method="post">
            <input type="file" name="file" id="fileUpload" size="50"/>
            <br/><br/>
            <input type="submit" value="Submit" id="submit"/>
            <br/><br/>
        </form>
        ${file}

    </div>

</div>
