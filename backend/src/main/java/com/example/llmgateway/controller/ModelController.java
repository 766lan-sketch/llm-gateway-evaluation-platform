package com.example.llmgateway.controller;

import com.example.llmgateway.dto.ChatRequest;
import com.example.llmgateway.dto.ChatResult;
import com.example.llmgateway.dto.CallLogView;
import com.example.llmgateway.dto.EvaluationStats;
import com.example.llmgateway.dto.ModelInfo;
import com.example.llmgateway.service.ModelRouterService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
// 这个 Controller 中所有接口都以 /api/models 开头。
@RequestMapping("/api/models")
@CrossOrigin(origins = "http://localhost:5175")
public class ModelController {
    private final ModelRouterService service;

    public ModelController(ModelRouterService service) { this.service = service; }

    @GetMapping
    // GET /api/models：让前端查询系统目前支持哪些模型。
    public List<ModelInfo> models() { return service.models(); }

    @PostMapping("/chat")
    // POST /api/models/chat：接收模型编号和问题，再交给路由器处理。
    public ChatResult chat(@Valid @RequestBody ChatRequest request) {
        return service.chat(request.model(), request.question());
    }

    @GetMapping("/calls")
    // GET /api/models/calls：查询数据库中最近 20 次模型调用。
    public List<CallLogView> recentCalls() {
        return service.recentCalls();
    }

    @GetMapping("/stats")
    // GET /api/models/stats：计算调用次数、成功率、平均耗时和模型对比。
    public EvaluationStats evaluationStats() {
        return service.evaluationStats();
    }
}
