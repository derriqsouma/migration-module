
<form id="moh361B-form" method="post" action="${ ui.actionLink("migrate", "moh361B","submit") }">
        <input type="file" name="file" id="fileUpload" size="50"/>
    <br/><br/>

    <div class="ke-form-footer">
        <button type="submit"><img src="${ ui.resourceLink("kenyaui", "images/glyphs/ok.png") }" />Submit</button>
    </div>
</form>
${file}

<script type="text/javascript">

    jQuery(function() {
        kenyaui.setupAjaxPost('moh361B-form', {
            onSuccess: function(data) {
                console.log(data);
                alert(data.file);
//                    ui.navigate('migrate', 'migrationHome');

            }
        });
    });
</script>