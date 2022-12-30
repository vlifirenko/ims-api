var token;

angular.module('ionicApp', ['ionic', 'angularFileUpload', 'ngResource'], function ($httpProvider) {
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
                        window.location = ("#/ims/auth");
                    } else if (response.status == 500) {
                        console.log(response.data);
                    }
                    return $q.reject(response);
                }
            };
        });

        $stateProvider
            .state('ims', {
                url: "/ims",
                abstract: true,
                templateUrl: "ims-menu.html"
            })
            .state('ims.auth', {
                url: "/auth",
                views: {
                    'menuContent': {
                        templateUrl: "auth.html",
                        controller: "AuthCtrl"
                    }
                }
            })
            .state('ims.register', {
                url: "/register",
                views: {
                    'menuContent': {
                        templateUrl: "register.html",
                        controller: "RegisterCtrl"
                    }
                }
            })
            .state('ims.filelisttag', {
                url: "/filelist/:tag",
                views: {
                    'menuContent': {
                        templateUrl: "filelist.html",
                        controller: "FileListCtrl"
                    }
                }
            })
            .state('ims.filelist', {
                url: "/filelist",
                views: {
                    'menuContent': {
                        templateUrl: "filelist.html",
                        controller: "FileListCtrl"
                    }
                }
            })
            .state('ims.graph', {
                url: "/graph",
                views: {
                    'menuContent': {
                        templateUrl: "graph.html",
                        controller: "GraphCtrl"
                    }
                }
            })
            .state('ims.graph-settings', {
                url: "/graph/settings",
                views: {
                    'menuContent': {
                        templateUrl: "graph-settings.html",
                        controller: "GraphSettingsCtrl"
                    }
                }
            })
            .state('ims.fileinfo', {
                url: "/fileinfo/:id",
                views: {
                    'menuContent': {
                        templateUrl: "fileinfo.html",
                        controller: "FileInfoCtrl"
                    }
                }
            })
            .state('ims.fileadd', {
                url: "/fileadd",
                views: {
                    'menuContent': {
                        templateUrl: "fileadd.html",
                        controller: "FileAddCtrl"
                    }
                }
            })
            .state('ims.fileedit', {
                url: "/fileedit/:id",
                views: {
                    'menuContent': {
                        templateUrl: "fileedit.html",
                        controller: "FileEditCtrl"
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


    .controller('MainCtrl', function ($scope, $ionicSideMenuDelegate) {
    })

    .controller('AuthCtrl', function ($scope, Auth, $location) {
        if (auth()) {
            $location.url("/ims/filelist");
        }

        $scope.errors = "";
        $scope.auth = {};

        $scope.submit = function () {
            $scope.errors = "";
            var correct = true;
            if ($scope.auth.email == undefined || $scope.auth.email.length == 0) {
                $scope.errors += "Поле \"E-mail\" не может быть пустым. ";
                correct = false;
            }
            if ($scope.auth.password == undefined || $scope.auth.password.length == 0) {
                $scope.errors += "Поле \"Пароль\" не может быть пустым.";
                correct = false;
            }
            if (!correct)
                return;

            Auth.get({email:$scope.auth.email, password:$scope.auth.password}, function(data) {
                if (typeof data == 'object') {
                    localStorage.setItem("token", data.token);
                    $location.url("ims/filelist");
                }
            }, function(response) {
                if (response.status == 400)
                    $scope.errors = "Не валидные данные";
                else if (status == 500)
                    $scope.errors = "Ошибка: " + response.data;
            });
        };

        $scope.register = function () {
            $location.url("/ims/register");
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

    .controller('FileListCtrl', function ($scope, Files, $location, $stateParams, $timeout) {
        $scope.clearTag = function () {
            $location.url("/ims/graph");
        };

        $scope.page = 0;
        $scope.pageSize = 20;

        $scope.tag = "";
        if ($stateParams.tag != undefined)
            $scope.tag = $stateParams.tag;

        $scope.errors = "";
        $scope.filelist = [];

        $scope.loadMore = function() {
            Files.get({token:token, offset:$scope.page * $scope.pageSize, limit:$scope.pageSize, tag:$scope.tag}, function(data) {
                if (typeof data == 'object') {
                    $scope.loaded = data.length <    $scope.pageSize;
                    $scope.filelist = $scope.filelist.concat(data);
                    $scope.page++;
                }
            }, function(response) {
                if (response.status == 500)
                    $scope.errors = "Ошибка: " + response.data;
            });
        };

        $scope.loadMore();
        $scope.loaded = true;

        $scope.add = function () {
            $location.url("/ims/fileadd");
        }

        $scope.toGraph = function () {
            $location.url("/ims/graph");
        }

        $scope.exit = function () {
            localStorage.clear();
            $location.url("/ims/auth");
        }
    })

    .controller('GraphCtrl', function ($scope, $location, Graph) {

        $scope.errors = "";
        $scope.graph = {};

        Graph.get({token:token}, function(data) {
            if (typeof data == 'object') {
                $scope.graph = data;
            }
        }, function(response) {
            if (response.status == 500)
                $scope.errors = "Ошибка: " + response.data;
        });

        $scope.toList = function () {
            $location.url("/ims/filelist");
        }

        $scope.exit = function () {
            localStorage.clear();
            $location.url("/ims/auth");
        }

        $scope.add = function () {
            $location.url("/ims/fileadd");
        }

        $scope.toSettings = function() {
            $location.url("/ims/graph/settings")
        }
    })

    .controller('FileInfoCtrl', function ($scope, $stateParams, FileInfo, $sce, $location, FileMeta, $ionicPopup) {
        $scope.errors = "";
        $scope.file = {};
        $scope.metalist = [];
        $scope.fileId = $stateParams.id;

        FileInfo.get({id:$stateParams.id, token:token}, function(data) {
            if (typeof data == 'object') {
                $scope.file = data;

                var pattern  = /\/files\/([^/]+)\/meta/ig;
                var parsed = pattern.exec($scope.file.meta)[1];

                $scope.file.meta = $sce.trustAsHtml("http://" + $scope.file.meta + "/?token=" + token);
                $scope.file.file = $sce.trustAsHtml("http://" + $scope.file.file + "/?token=" + token);

                FileMeta.get({meta:parsed, token:token}, function(data) {
                    $scope.metalist = data;
                }, function(response) {
                    if (response.status == 500)
                        $scope.errors = "Ошибка: " + response.data;
                });
            }
        }, function(response) {
            if (response.status == 500)
                $scope.errors = "Ошибка: " + response.data;
        });

        $scope.getMeta = function () {
            window.open($scope.file.meta, '_self');
        }

        $scope.getFile = function () {
            window.open($scope.file.file, '_self');
        }

        $scope.edit = function () {
            $location.url("/ims/fileedit/" + $scope.fileId);
        }

        $scope.delete = function () {
            $ionicPopup.confirm({
                title: 'Deleteing file',
                template: 'Are you sure you want to delete this file?'
            }).then(function(res) {
                if (res) {
                    FileInfo.delete({id:$stateParams.id, token:token}, function(data) {
                        $location.url("/ims/filelist");
                    }, function(response) {
                        if (response.status == 500)
                            $scope.errors = "Ошибка: " + response.data;
                    });
                }
            });
        }

        $scope.list = function () {
            $location.url("/ims/filelist")
        }
    })

    .controller('GraphSettingsCtrl', function($scope, $location) {
        if (!auth()) {
            $location.url("/ims/auth");
        }
        $scope.settings = {};
        $scope.settings.minDiam = localStorage.getItem("minDiam") == null ? 0 : localStorage.getItem("minDiam");
        $scope.settings.minPow = localStorage.getItem("minDiam") == null ? 0 : localStorage.getItem("minPow");
        $scope.settings.minLength = localStorage.getItem("minDiam") == null ? 0 : localStorage.getItem("minLength");
        $scope.settings.c1 = localStorage.getItem("c1") == null ? 100 : localStorage.getItem("c1");
        $scope.settings.c2 = localStorage.getItem("c2") == null ? 100 : localStorage.getItem("c2");
        $scope.settings.c3 = localStorage.getItem("c3") == null ? 100 : localStorage.getItem("c3");

        $scope.back = function() {
            $location.url("/ims/graph");
        }

        $scope.save = function() {
            localStorage.setItem("minDiam", $scope.settings.minDiam);
            localStorage.setItem("minPow", $scope.settings.minPow);
            localStorage.setItem("minLength", $scope.settings.minLength);
            localStorage.setItem("c1", $scope.settings.c1);
            localStorage.setItem("c2", $scope.settings.c2);
            localStorage.setItem("c3", $scope.settings.c3);
            $location.url("/ims/graph");
        }
    })

    .controller('FileAddCtrl', function ($scope, $upload, $timeout, $location) {
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
                    });
                }, function (response) {
                    console.log(response);
                    if (response.status == 302) {
                        $location.url("/ims/filelist");
                    }
                }, function (evt) {
                    console.log(Math.min(100, parseInt(100.0 * evt.loaded / evt.total)));
                });
            }
        }
    })

    .controller('FileEditCtrl', function ($scope, $stateParams, $sce, $location, $upload, $timeout) {
        if (!auth()) {
            $location.url("/ims/auth");
        }
        $scope.file = {};
        $scope.fileId = $stateParams.id;

        var files;

        $scope.onFileSelect = function ($files) {
            files = $files;
        }

        $scope.upload = function ($files) {
            var meta = $scope.meta;
            if (meta == undefined) {
                $scope.errors = "Поле с тэгами обязательное";
                return;
            }
            meta = formatMeta(meta);

            data = {token: token, meta: meta};
            for (var i = 0; i < files.length; i++) {
                var file = files[i];
                $scope.upload = $upload.upload({
                    url: "/api/v1/files/" + $scope.fileId + "/file",
                    //method: 'POST' or 'PUT',
                    data: data,
                    file: file
                });
                $scope.upload.then(function (response) {
                    $timeout(function () {
                        console.log(response);
                        $location.url("/ims/filelist");
                    });
                }, function (response) {
                    console.log(response);
                    if (response.staus == 302) {
                        $location.url("/ims/filelist");
                    }
                }, function (evt) {
                    console.log(Math.min(100, parseInt(100.0 * evt.loaded / evt.total)));
                });
            }
        };

        $scope.list = function () {
            $location.url("/ims/filelist")
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
    result = "{\"tags\":[" + result + "], \"title\":\"" + meta.title.trim() + "\"}";
    return result;
}

function showListTag(tag) {
    window.location = "#/ims/filelist/" + tag;
}

function renderGraph(nodes, links) {
    var minDiam = localStorage.getItem("minDiam") == null ? 1 : parseInt(localStorage.getItem("minDiam"));
    var minPow = localStorage.getItem("minPow") == null ? 1 : parseInt(localStorage.getItem("minPow"));
    var minLength = localStorage.getItem("minLength") == null ? 1 : parseInt(localStorage.getItem("minLength"));
    var c1 = localStorage.getItem("c1") == null ? 100 : parseInt(localStorage.getItem("c1"));
    var c2 = localStorage.getItem("c2") == null ? 100 : parseInt(localStorage.getItem("c2"));
    var c3 = localStorage.getItem("c3") == null ? 100 : parseInt(localStorage.getItem("c3"));

    var graph = Viva.Graph.graph();
    for (var i=0; i<nodes.length; i++) {
        graph.addNode(nodes[i].name, nodes[i]);
    }
    for (var i=0; i<links.length; i++) {
        graph.addLink(links[i].node1, links[i].node2, { connectionStrength: minPow + Math.sqrt(links[i].coefficient * c2)});
    }
    var idealLength = 400;
    var layout = Viva.Graph.Layout.forceDirected(graph, {
                springLength : idealLength,
                springCoeff : 0.0005,
                dragCoeff : 0.02,
                gravity : -1.2,
                springTransform: function (link, spring) {
                    spring.length = minLength + Math.sqrt(link.data.connectionStrength*c2);
                }
            });
    var graphics = Viva.Graph.View.svgGraphics();
    graphics.node(function(node) {
        var radius = minDiam + Math.sqrt(node.data.coefficient*c1);
        var circle = Viva.Graph.svg('circle')
                     .attr('r', radius)
                     .attr('fill', node.data.color)
                     .attr('ondblclick', "showListTag('" + node.data.name + "');");
        var text = Viva.Graph.svg('text')
                  .attr('font-size', 12)
                  .attr('y', radius + 15)
                  .attr('x', -7 + node.data.coefficient * 3)
                  .text(node.data.name);
        var ui = Viva.Graph.svg('g');
        ui.append(circle);
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
            layout: layout
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