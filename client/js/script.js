var SERVER_URL = "http://localhost:8080";

var token;

angular.module('imsApp', ['ui.router', 'ngResource'], function ($httpProvider) {
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
                .state('ims', {
                    url: "/ims",
                    abstract: true,
                    templateUrl: "ims-menu.html"
                })
                .state('start', {
                    url: "/start",
                    abstract: true,
                    templateUrl: "start-menu.html"
                })
                .state('start.auth', {
                    url: "/auth",
                    views: {
                        'menuContent': {
                            templateUrl: "auth.html",
                            controller: "AuthCtrl"
                        }
                    }
                })
                .state('start.register', {
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
                .state('ims.logout', {
                    url: "/logout",
                    views: {
                        'menuContent': {
                            controller: "LogoutCtrl"
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
            ;

            $urlRouterProvider.otherwise("/ims/filelist");
        })

    .factory('Auth', function($resource) {
        return $resource(SERVER_URL + '/api/v1/auth?email=:email&password=:password');
    })
    .factory('Register', function($resource) {
        return $resource(SERVER_URL + '/api/v1/register');
    })
    .factory('Files', function($resource) {
        return $resource(SERVER_URL + '/api/v1/files?token=:token&offset=:offset&limit=:limit'
            , {}, {'get': {method: 'GET', isArray: true}});
    })
    .factory('Graph', function($resource) {
        return $resource(SERVER_URL + '/api/v1/graph?token=:token');
    })
    .factory('FileInfo', function($resource) {
        return $resource(SERVER_URL + '/api/v1/files/:id/?token=:token');
    })
    .factory('FileMeta', function($resource) {
        return $resource(SERVER_URL + '/api/v1/files/:meta/meta/?token=:token');
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

    .controller('FileListCtrl', function ($scope, Files, $location, $stateParams, Graph) {
        auth();
        var tag = "";
        if ($stateParams.tag != undefined)
            tag = $stateParams.tag;

        $scope.errors = "";
        $scope.filelist = [];

        Files.get({token:token, offset:"0", limit:"20", tag:tag}, function(data) {
            if (typeof data == 'object') {

                for (var i = 0; i < data.length; i++) {
                    var d = new Date(data[i].created);
                    data[i].created = d.toUTCString();
                    data[i].tags = data[i].meta.tags.join(', ');
                }
                $scope.filelist = data;
            }
        }, function(response) {
            if (response.status == 500)
                $scope.errors = "Ошибка: " + response.data;
        });

        Graph.get({token:token}, function(data) {
            if (typeof data == 'object') {
                $scope.graph = data;
            }
        }, function(response) {
            if (response.status == 500)
                $scope.errors = "Ошибка: " + response.data;
        });

        var offset = 20;

        $scope.loadMore = function() {
            Files.get({token:token, offset:offset, limit:"20", tag:tag}, function(data) {
                if (typeof data == 'object') {
                    $scope.filelist = $scope.filelist.concat(data);
                    $scope.$broadcast('scroll.infiniteScrollComplete');
                    offset+=20;
                }
            }, function(response) {
                if (response.status == 500)
                    $scope.errors = "Ошибка: " + response.data;
            });
        };

        $scope.$on('$stateChangeSuccess', function() {
            $scope.loadMore();
        });
    })

    .controller('GraphSettingsCtrl', function($scope, $location) {
        if (!auth()) {
            $location.url("/start/auth");
        }
        $scope.settings = {};
        $scope.settings.minDiam = localStorage.getItem("minDiam") == null ? 0 : localStorage.getItem("minDiam");
        $scope.settings.minPow = localStorage.getItem("minDiam") == null ? 0 : localStorage.getItem("minPow");
        $scope.settings.minLength = localStorage.getItem("minDiam") == null ? 0 : localStorage.getItem("minLength");
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
;