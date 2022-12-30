package com.ims.helpers;

import com.ims.vos.GraphVo;
import com.ims.vos.LinkVo;
import com.ims.vos.NodeVo;
import com.ims.vos.UserVo;
import org.gephi.graph.api.*;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.exporter.spi.GraphExporter;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class GephiWriter {

	public static boolean delete(UserVo userVo) {
		String tempDir = System.getProperty("java.io.tmpdir")
				+ System.getProperty("file.separator");
		File file = new File(tempDir + userVo.email + ".gefx");
		return file.delete();
	}

	public static File generateGexfFile(UserVo userVo, GraphVo graphVo)
			throws IOException {
		ProjectController pc = Lookup.getDefault().lookup(
				ProjectController.class);
		pc.newProject();
		GraphModel graphModel = Lookup.getDefault()
				.lookup(GraphController.class).getModel();
		Workspace workspace = pc.getCurrentWorkspace();

		DirectedGraph directedGraph = graphModel.getDirectedGraph();
		HashMap<String, Node> nodes = new HashMap<String, Node>();
		for (NodeVo nodeVo : graphVo.nodes) {
			Node n = graphModel.factory().newNode(nodeVo.name);
			n.getNodeData().setLabel(nodeVo.name);
			Color color = ColorUtil.decodeHtmlColorString(nodeVo.color);
			n.getNodeData().setColor((float) color.getRed() / 255f,
					(float) color.getGreen() / 255f,
					(float) color.getBlue() / 255f);
			n.getNodeData().setSize(nodeVo.coefficient);
			nodes.put(nodeVo.name, n);
			directedGraph.addNode(n);
		}

		for (LinkVo linkVo : graphVo.links) {
			Edge e = graphModel.factory().newEdge(nodes.get(linkVo.node1),
					nodes.get(linkVo.node2), linkVo.coefficient, true);
			e.getEdgeData().setColor(.80f, .80f, .80f);
			directedGraph.addEdge(e);
		}

		AutoLayout autoLayout = new AutoLayout(5, TimeUnit.SECONDS);
		autoLayout.setGraphModel(graphModel);
		ForceAtlas2 layout = new ForceAtlas2(null);
		autoLayout.addLayout(layout, 1f);
		autoLayout.execute();

		ExportController ec = Lookup.getDefault()
				.lookup(ExportController.class);
		GraphExporter exporter = (GraphExporter) ec.getExporter("gexf");
		exporter.setExportVisible(true);
		exporter.setWorkspace(workspace);
		String tempDir = System.getProperty("java.io.tmpdir")
				+ System.getProperty("file.separator");
		File graphFile = new File(tempDir + userVo.email + ".gexf");
		ec.exportFile(graphFile, exporter);
		return graphFile;
	}

	public static boolean gexfFileExists(UserVo userVo) {
		String tempDir = System.getProperty("java.io.tmpdir")
				+ System.getProperty("file.separator");
		File file = new File(tempDir + userVo.email + ".gexf");
		return file.exists();
	}
}
