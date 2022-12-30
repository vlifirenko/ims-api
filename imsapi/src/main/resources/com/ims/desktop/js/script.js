var token;

angular.module('imsApp', ['ui.router', 'ngResource', 'angularFileUpload'], function ($httpProvider) {
    $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded;charset=utf-8';
})

    .directive('graph', function() {
        return {
          templateUrl: 'graphdir.html',
          link: function ($scope, element, attrs) {
            attrs.$observe('gr', function(value) {
                var graph = JSON.parse(value);
                if (graph.nodes != undefined && graph.links != undefined)
                    renderGraph(graph.nodes, graph.links)
            });
          }
        };
    })

    .directive('sigmagraph', function() {
        return {
          templateUrl: 'sigmagraphdir.html',
          link: function ($scope, element, attrs) {
            attrs.$observe('gr', function(value) {
                var graph = JSON.parse(value);
                if (graph.nodes != undefined && graph.links != undefined)
                    //renderSigmaGraph(graph.nodes, graph.links)
                    renderSigmaGexfGraph();
            });
          }
        };
    })

    .config(function ($stateProvider, $urlRouterProvider, $httpProvider) {

            $httpProvider.interceptors.push(function($q) {
                return {
                    'request' : function(config) {
                        return config || $q.when(config);
                    },
                    'responseError' : function(response) {
                        if (response.status == 401) {
                            window.location = ("#/start/auth");
                        } else if (response.status == 500) {
                            console.log(response.data);
                        }
                        return $q.reject(response);
                    }
                };
            });

            $stateProvider
                .state('start', {
                    url: "/start",
                    abstract: true,
                    templateUrl: "start-menu.html"
                })
                .state('start.auth', {
                    url: "/auth",
                    views: {
                        'mainContent': {
                            templateUrl: "auth.html",
                            controller: "AuthCtrl"
                        }
                    }
                })
                .state('start.register', {
                    url: "/register",
                    views: {
                        'mainContent': {
                            templateUrl: "register.html",
                            controller: "RegisterCtrl"
                        }
                    }
                })
                .state('ims', {
                    url: "/ims",
                    abstract: true,
                    templateUrl: "ims-menu.html",
					controller: "FileListCtrl"
                })
                .state('ims.filelist', {
                    url: "/filelist",
                    views: {
                        'mainContent': {
                            templateUrl: "filelist.html"
                        }
                    }
                })
                .state('ims.logout', {
                    url: "/logout",
                    views: {
                        'mainContent': {
                            controller: "LogoutCtrl"
                        }
                    }
                })
                .state('ims.upload', {
                    url: "/upload",
                    views: {
                        'mainContent': {
                            templateUrl: "upload.html",
                            controller: "UploadCtrl"
                        }
                    }
                })
                .state('ims.create', {
                    url: "/create",
                    views: {
                        'mainContent': {
                            templateUrl: "create.html",
                        }
                    }
                })
                .state('ims.activity', {
                    url: "/activity",
                    views: {
                        'mainContent': {
                            templateUrl: "activity.html",
                        }
                    }
                })
                .state('ims.info', {
                    url: "/info",
                    views: {
                        'mainContent': {
                            templateUrl: "info.html",
                        }
                    }
                })
                .state('ims.graph-settings', {
                    url: "/graph/settings",
                    views: {
                        'mainContent': {
                            templateUrl: "graph-settings.html",
                            controller: "GraphSettingsCtrl"
                        }
                    }
                })
            ;

            $urlRouterProvider.otherwise("/ims/filelist");
        })

    .factory('Auth', function($resource) {
        return $resource('/api/v1/auth?email=:email&password=:password');
    })
    .factory('Register', function($resource) {
        return $resource('/api/v1/register');
    })
    .factory('Files', function($resource) {
        return $resource('/api/v1/files?token=:token&offset=:offset&limit=:limit'
            , {}, {'get': {method: 'GET', isArray: true}});
    })
    .factory('Graph', function($resource) {
        return $resource('/api/v1/graph?token=:token');
    })
    .factory('FileInfo', function($resource) {
        return $resource('/api/v1/files/:id/?token=:token');
    })
    .factory('FileMeta', function($resource) {
        return $resource('/api/v1/files/:meta/meta/?token=:token');
    })

    .controller('MainCtrl', function ($scope) {
        })

    .controller('AuthCtrl', function ($scope, Auth, $location) {
        if (auth()) {
            $location.url("/ims/filelist");
        }

        $scope.errors = "";
        $scope.auth = {};

        $scope.submit = function () {
            $scope.errors = "";
            Auth.get({email:$scope.auth.email, password:$scope.auth.password}, function(data) {
                if (typeof data == 'object') {
                    localStorage.setItem("token", data.token);
                    $location.url("/ims/filelist");
                }
            }, function(response) {
                if (response.status == 400)
                    $scope.errors = "Не валидные данные";
                else if (status == 500)
                    $scope.errors = "Ошибка: " + response.data;
            });
        };

        $scope.register = function () {
            $location.url("/start/register");
        }
    })

    .controller('RegisterCtrl', function ($scope, Register, $location) {
        if (auth()) {
            $location.url("/ims/filelist");
        }

        $scope.errors = "";
        $scope.register = {};

        $scope.submit = function () {
            $scope.errors = "";
            var correct = true;
            if ($scope.register.email == undefined || $scope.register.email.length == 0) {
                $scope.errors += "Поле \"E-mail\" не может быть пустым. ";
                correct = false;
            }
            if ($scope.register.password == undefined || $scope.register.password.length == 0) {
                $scope.errors += "Поле \"Пароль\" не может быть пустым. ";
                correct = false;
            }
            if ($scope.register.password2 == undefined || $scope.register.password2.length == 0) {
                $scope.errors += "Поле \"Повторить пароль\" не может быть пустым.";
                correct = false;
            }
            if (!correct)
                return;
            if ($scope.register.password !== $scope.register.password2) {
                $scope.errors += "Пароли не совпадаются";
                correct = false;
            }
            if (!correct)
                return;

            Register.save("email=" + $scope.register.email + "&password=" + $scope.register.password, function(data) {
                if (typeof data == 'object') {
                    localStorage.setItem("token", data.token);
                    $location.url("/ims/filelist");
                }
            }, function(response) {
                if (response.status == 400)
                    $scope.errors = "Не валидные данные";
                else if (status == 500)
                    $scope.errors = "Ошибка: " + response.data;
            });
        };
    })

    .controller('FileListCtrl', function ($scope, Files, $location, Graph, $sce) {
        auth();
        $scope.tag = {};
		$scope.errors = "";
        $scope.filelist = [];
		$scope.tagColors = [];
        $scope.offset = 0;
		$scope.limit = 20;
		$scope.sort = {};

		$scope.sortValues = [{
          id: "meta.title",
          label: 'названию'
        }, {
          id: "meta.extension",
          label: 'расширению'
        }, {
           id: "created",
           label: 'дате создания'
        }];


		$scope.getTagColor = function(tag) {
			if ($scope.tagColors[tag])
				return $scope.tagColors[tag];
		
			for (var i = 0; i < $scope.graph.nodes.length; i++) {
				if ($scope.graph.nodes[i].name == tag) {
					$scope.tagColors[tag] = $scope.graph.nodes[i].color;
					return $scope.tagColors[tag];
				}
			}
			
			return '#eee';
		};

        $scope.deliberatelyTrustDangerousSnippet = function(snippet) {
           return $sce.trustAsHtml(snippet);
        };
		
		$scope.getFiles = function(tag, offset, sort) {
			$scope.offset = offset;
			$scope.tag = tag;
			var filter = {token:token, offset:$scope.offset, limit:$scope.limit};
			if (tag)
				filter.tag = tag.name;
			if (sort) {
			    if (sort.sortField.id != undefined)
			        filter.sortField = sort.sortField.id;
			    else
			        filter.sortField = sort.sortField;
                filter.sortDesc = sort.sortDesc;
			}

			Files.get(filter, function(data) {
                console.log(data);
				if (!data || data.length == 0) {
					if ($scope.offset == 0)
						$scope.filelist = [];

					$scope.loaded = true;
					return;
				}

				for (var i = 0; i < data.length; i++) {
					data[i].meta.tags = data[i].meta.tags.sort();
					for (var j = 0; j < data[i].meta.tags.length; j++) {
						data[i].meta.tags[j] = {name: data[i].meta.tags[j], color: $scope.getTagColor(data[i].meta.tags[j])}
					}
				}

				if ($scope.offset == 0)
					$scope.filelist = data;
				else 
					$scope.filelist = $scope.filelist.concat(data);
				
				$scope.loaded = (data.length != $scope.limit);
				
				$scope.offset += data.length;
			}, function(response) {
				if (response.status == 500)
					$scope.errors = "Ошибка: " + response.data;
			});
		};

		$scope.$on('refreshList', function(event) {
		    $scope.getFiles({}, 0);
		});

        Graph.get({token:token}, function(data) {
            if (typeof data == 'object') {
                $scope.graph = data;
				$scope.getFiles({}, 0);
            }
        }, function(response) {
            if (response.status == 500)
                $scope.errors = "Ошибка: " + response.data;
        });

        $scope.showSortOptions;
        $scope.showSortList = function() {
            $scope.showSortOptions = !$scope.showSortOptions;
            $scope.sort.field = "created";
            $scope.sort.desc = true;
            $scope.applySort();
        }

        $scope.applySort = function() {
            var sort = {};
            sort.sortField = $scope.sort.field;
            if ($scope.sort.desc == undefined || $scope.sort.desc == 0)
                sort.sortDesc = 1;
            else
                sort.sortDesc = 0;
            $scope.offset = 0;
            $scope.getFiles($scope.tag, $scope.offset, sort);
        }

        $scope.loadMore = function() {
            $scope.getFiles($scope.tag, $scope.offset);
        };
		
		$scope.getClassByFileName = function(fileName) {
			var idx = fileName.lastIndexOf('.');
			var ext = fileName.substr(idx + 1).toLowerCase();
			
			var types = {
				  'glyphicon-film':     ['avi', 'mp4']
				, 'glyphicon-list-alt': ['pdf', 'txt', 'odt', 'css', 'js', 'doc', 'docx', 'xls', 'sql', 'xlsx', 'csv']
				, 'glyphicon-picture':  ['jpg', 'jpeg', 'gif', 'png', 'pxd', 'odg', 'odp', 'ods', 'pptx', 'ppt']
				, 'glyphicon-music':    ['mp3']
				, 'glyphicon-globe':    ['html', 'htm', 'mhtml']
			};
			
			for (var clazz in types) {
				for (var i = 0; i < types[clazz].length; i++) {
					if (types[clazz][i].indexOf(ext) != -1)
						return clazz;
				}
			}
			
			return 'glyphicon-file';
		};
    })

    .controller('GraphSettingsCtrl', function($scope, $location) {
        if (!auth()) {
            $location.url("/start/auth");
        }
		
        $scope.settings = {};
        $scope.settings.minDiam = localStorage.getItem("minDiam") == null ? 1 : localStorage.getItem("minDiam");
        $scope.settings.minPow = localStorage.getItem("minDiam") == null ? 1 : localStorage.getItem("minPow");
        $scope.settings.minLength = localStorage.getItem("minDiam") == null ? 1 : localStorage.getItem("minLength");
        $scope.settings.c1 = localStorage.getItem("c1") == null ? 100 : localStorage.getItem("c1");
        $scope.settings.c2 = localStorage.getItem("c2") == null ? 100 : localStorage.getItem("c2");
        $scope.settings.c3 = localStorage.getItem("c3") == null ? 100 : localStorage.getItem("c3");

        $scope.submit = function() {
            localStorage.setItem("minDiam", $scope.settings.minDiam);
            localStorage.setItem("minPow", $scope.settings.minPow);
            localStorage.setItem("minLength", $scope.settings.minLength);
            localStorage.setItem("c1", $scope.settings.c1);
            localStorage.setItem("c2", $scope.settings.c2);
            localStorage.setItem("c3", $scope.settings.c3);
            $location.url("/ims/filelist");
        }
    })

    .controller('LogoutCtrl', function($scope, $location) {
        localStorage.clear();
        $location.url("/start/auth");
    })

    .controller('UploadCtrl', function($scope, $location, $upload, $timeout) {
        if (!auth()) {
            $location.url("/ims/auth");
        }

        $scope.errors = "";
        $scope.file = {};

        var guid = (function () {
            function s4() {
                return Math.floor((1 + Math.random()) * 0x10000)
                    .toString(16)
                    .substring(1);
            }

            return function () {
                return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
                    s4() + '-' + s4() + s4() + s4();
            };
        })();

        var files;

        $scope.onFileSelect = function ($files) {
            files = $files;
        }

        $scope.upload = function () {
            var meta = $scope.file.meta;
            if (meta == undefined) {
                $scope.errors = "Поле с тэгами обязательное";
                return;
            }
            meta.extension = files[0].name.split('.').pop();
            meta = formatMeta(meta);
            data = {token: token, meta: meta};
            for (var i = 0; i < files.length; i++) {
                var file = files[i];
                $scope.upload = $upload.upload({
                    url: "/api/v1/files/" + guid() + "/file",
                    //method: 'POST' or 'PUT',
                    data: data,
                    file: file
                });
                $scope.upload.then(function (response) {
                    $timeout(function () {
                        $location.url("/ims/filelist");
                        $scope.$emit('refreshList');
                    });
                }, function (response) {
                    if (response.status == 302) {
                        $location.url("/ims/filelist");
                        $scope.$emit('refreshList');
                    }
                }, function (evt) {
                    console.log(Math.min(100, parseInt(100.0 * evt.loaded / evt.total)));
                });
            }
        }
    })

;

function auth() {
    if (localStorage.getItem("token") != undefined) {
        token = localStorage.getItem("token");
        return true;
    }

    return false;
}

function formatMeta(meta) {
    var arr = meta.tags.split(",");
    var result = "\"" + arr[0] + "\"";
    for (var i = 1; i < arr.length; i++) {
        result += ",\"" + arr[i].trim() + "\"";
    }
    ;
    result = "{\"tags\":[" + result + "], \"title\":\"" + meta.title.trim() + "\", \"extension\":\"" + meta.extension.trim() + "\"}";
    return result;
}

function showListTag(tag) {
	angular.element(document.getElementById('filelist')).scope().getFiles({name: tag}, 0);
}

var graph;
function renderGraph(nodes, links) {
    var minDiam   = localStorage.getItem("minDiam") == null   ? 10   : parseInt(localStorage.getItem("minDiam"));
    var c1        = localStorage.getItem("c1") == null        ? 1000 : parseInt(localStorage.getItem("c1"));
    var minPow    = localStorage.getItem("minPow") == null    ? 50   : parseInt(localStorage.getItem("minPow"));
    var c2        = localStorage.getItem("c2") == null        ? 1000 : parseInt(localStorage.getItem("c2"));
    var minLength = localStorage.getItem("minLength") == null ? 50   : parseInt(localStorage.getItem("minLength"));
    var c3        = localStorage.getItem("c3") == null        ? 1000 : parseInt(localStorage.getItem("c3"));

    graph = Viva.Graph.graph();
    for (var i=0; i<nodes.length; i++) {
        graph.addNode(nodes[i].name, nodes[i]);
    }
    for (var i=0; i<links.length; i++) {
        graph.addLink(links[i].node1, links[i].node2, { connectionStrength: minPow + Math.sqrt(links[i].coefficient * c2)});
    }
	
    var layout = Viva.Graph.Layout.forceDirected(graph, {
                springLength : 400,
                springCoeff : 0.0009,
                dragCoeff : 0.02,
                gravity : 0,
                springTransform: function (link, spring) {
                    spring.length = minLength + Math.sqrt(link.data.connectionStrength*c2);
                }
            });

    var graphics = Viva.Graph.View.svgGraphics();
    graphics.node(function(node) {
        var radius = minDiam + Math.sqrt(node.data.coefficient * c1);
        var circle = Viva.Graph.svg('circle')
                     .attr('r', radius)
                     .attr('fill', node.data.color)
                     .attr('ondblclick', "showListTag('" + node.data.name + "');");
		var label = (node.data.name.length > 5) ? node.data.name.substring(0, 5) + '...' : node.data.name;
        var text = Viva.Graph.svg('text')
                  .attr('y', radius + 15)
                  .attr('x', -7 + node.data.coefficient * 3)
                  .text(label);
		var text1 = Viva.Graph.svg('text')
                  .attr('class', 'underlay')
                  .attr('y', radius + 15)
                  .attr('x', -7 + node.data.coefficient * 3)
                  .text(label);
        var ui = Viva.Graph.svg('g');
        ui.append(circle);
        ui.append(text1);
		ui.append(text);
        return ui;
    })
	
    .placeNode(function(nodeUI, pos, node){
        nodeUI.attr('transform',
            'translate(' +
                  (pos.x - (node.data.coefficient/2)) + ',' + (pos.y - node.data.coefficient/2) +
            ')');
    });
    var renderer = Viva.Graph.View.renderer(graph,
        {
            container: document.getElementById('graph'),
            graphics: graphics,
            layout: layout,
            prerender  : 1200
        });
    renderer.run();
}

function renderSigmaGraph(nodes, edges) {
    var g = {nodes: [], edges: []};

    for (i = 0; i < nodes.length; i++) {
      g.nodes.push({
        id: nodes[i].name,
        label: nodes[i].name,
        x:  Math.cos(2 * i * Math.PI / nodes.length),
        y:  Math.sin(2 * i * Math.PI / nodes.length),
        size: Math.random()*50,
        color: nodes[i].color
      });
    }

    for (i = 0; i < edges.length; i++) {
        g.edges.push({
            id: 'e' + i,
            source: edges[i].node1,
            target: edges[i].node2,
            color: '#ccc'
        });
    }

    var s = new sigma({
      graph: g,
      container: 'sigmagraph',
      settings: {
        doubleClickEnabled: true
      }
    });
    s.startForceAtlas2({worker: true, barnesHutOptimize: false});
    s.bind('doubleClickNode', function(e) {
      showListTag(e.data.node.id);
    });
}

function renderSigmaGexfGraph() {
    path = '/api/v1/graph?format=gexf&token=' + token;
    var s = new sigma({
      container: 'sigmagraph',
      settings: {
        doubleClickEnabled: true
      }
    });
    sigma.parsers.gexf(
      path, s,
      function() {
        s.refresh();
      }
    );
}