<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>
<html>
    <body>
        <form action="migrateData.form" method="post">
            <input type="file" name="file" id="fileUpload" size="50"/>

            <br/><br/>
            <input type="submit" value="Submit" id="submit"/>
            <br/><br/>
        </form>

    </body>
    ${thefile}
    ${error}
</html>

<%@ include file="/WEB-INF/template/footer.jsp"%>