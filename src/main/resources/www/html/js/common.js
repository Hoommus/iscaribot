$(function () {
	var options = { "container" : "body", 
		"template":'<div class="popover matte-light" role="tooltip"><div class="blur-bible-bg trice"></div><div class="arrow"></div><h3 class="popover-header"></h3><div class="popover-body"></div></div>'
	} 
  $('[data-toggle="popover"]').popover(options)
})