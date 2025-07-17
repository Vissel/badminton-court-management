// src/LoginPage.js
import { useContext, useEffect, useState } from "react";
import {
  Form,
  Row,
  Col,
  Button,
  Container,
  InputGroup,
  FormLabel,
  Table,
} from "react-bootstrap";
import api from '../api/index'
import { AuthContext } from "../context/AuthContext";
import axios from "axios";

function SetupPage() {
  const { csrfToken } = useContext(AuthContext);

  const currentHost = `${window.location.protocol}//${window.location.hostname}`;
  const localHost = "http://localhost:9080";
  const context = "bad-court-management-dev";

  const [totalCourt, setTotalCourt] = useState(7);
  const [costInPerson, setCostInPerson] = useState(12000);
  // const [shuttleName, setShuttleName] = useState("");
  // const [shuttleCost, setShuttleCost] = useState("");
  const [shuttleBall, setShuttleBall] = useState([]);
  const [tableServices, setTableServices] = useState([]);
  const [services, setServices] = useState([]);
  const handleAdd = () => {
    setServices([...services, { id: Date.now(), serviceName: "", cost: "" }]);
  };
  const handleRemove = (id) => {
    setServices(services.filter((service) => service.id !== id));
  };

  // for shuttle ball
  const addShuttleBall = () => {
    setShuttleBall([
      ...shuttleBall,
      { id: Date.now(), shuttleName: "", shuttleCost: "" },
    ]);
  };
  const removeShuttleBall = (id) => {
    setShuttleBall(shuttleBall.filter((ball) => ball.id !== id));
  };
  const handleBallChange = (id, field, value) => {
    setShuttleBall(
      shuttleBall.map((ball) =>
        ball.id === id ? { ...ball, [field]: value } : ball
      )
    );
  };
  const handleServiceChange = (id, field, value) => {
    setServices(
      services.map((service) =>
        service.id === id ? { ...service, [field]: value } : service
      )
    );
  };
  const handleSave = async () => {
    // // setServices(0,"costInPerson", costInPerson)
    // setServices([
    //   ...services,
    //   { id: Date.now(), serviceName: "costInPerson", cost: costInPerson },
    // ]);
    services.push( { id: Date.now(), serviceName: "costInPerson", cost: costInPerson });

    const payload = {
      totalCourt: totalCourt,
      shuttleBalls: shuttleBall.map(({ shuttleName, shuttleCost }) => ({
        shuttleName: shuttleName,
        shuttleCost: parseFloat(shuttleCost),
      })),
      services: services.map(({ serviceName, cost }) => ({
        serviceName: serviceName,
        cost: parseFloat(cost),
      })),
    };

    try {
      const response = await api.post(
        `/api/addSetupService`,
        payload,{}
      );
      if (response.status === 200) {
        alert("Services saved successfully!");
        // window.location.reload();
      }
    } catch (error) {
      alert("Failed to save services.");
    }
  };

  const fetchEntries = async () => {
    try {
      const res = await api.get("/api/getSetupServices");
      console.log(`return code:${res.status}`);
      console.log(`table:${tableServices.length}`);
      if (res.status === 200 && res.data !== "") {
        setTotalCourt(res.data.totalCourt);
        setCostInPerson(res.data.costInPerson);
        // setShuttleBall(res.data.shuttleBalls);
        // setTableServices(res.data.services);
      }
    } catch (error) {
      console.error(error);
    }
  };

  useEffect(() => {
    fetchEntries();
  }, []);

  return (
    <Container>
      <Row>
        <Col sm={4}>
          <h1>Setup page</h1>
        </Col>
        <Col sm={{ span: 4, offset: 4 }}>
          <Button variant="primary" onClick={handleSave}>
            Save setting
          </Button>
        </Col>
      </Row>
      <Form>
        <Form.Group as={Row} className="md-3">
          <Form.Label column sm="1">
            {" "}
            Tong san:
          </Form.Label>
          <Col sm="4">
            <InputGroup className="mb-3">
              <Form.Control
                type="number"
                defaultValue="7"
                value={totalCourt}
                onChange={(e) => setTotalCourt(e.target.value)}
              />
              <InputGroup.Text>san</InputGroup.Text>
            </InputGroup>
          </Col>
        </Form.Group>
        <Form.Group as={Row} className="mb-3">
          <Form.Label column sm="1">
            Tien san:
          </Form.Label>
          <Col sm="4">
            <InputGroup className="mb-3">
              <Form.Control
                type="number"
                defaultValue="12000"
                value={costInPerson}
                onChange={(e) => setCostInPerson(e.target.value)}
              />
              <InputGroup.Text>vnd/nguoi</InputGroup.Text>
            </InputGroup>
          </Col>
        </Form.Group>

        {/* Add shuttleBall */}
        <Row className="row-space">
          <Col>
            <Button variant="success" onClick={addShuttleBall} className="me-2">
              + Add Shuttle ball
            </Button>
          </Col>
        </Row>

        {shuttleBall.map((ball, index) => (
          <Form.Group
            as={Row}
            key={ball.id}
            className="align-items-center mb-2"
          >
            <Form.Label column sm="1">
              Loai cau:
            </Form.Label>
            <Col sm={3}>
              <Form.Control
                type="text"
                placeholder="VinaStar"
                value={ball.shuttleName}
                onChange={(e) =>
                  handleBallChange(ball.id, "shuttleName", e.target.value)
                }
              />
            </Col>
            <Col sm={3}>
              <InputGroup className="mb-2">
                <Form.Control
                  type="number"
                  defaultValue="26000"
                  value={ball.shuttleCost}
                  onChange={(e) =>
                    handleBallChange(ball.id, "shuttleCost", e.target.value)
                  }
                />
                <InputGroup.Text>vnd/trai</InputGroup.Text>
              </InputGroup>
            </Col>
            <Col md={1}>
              <Button
                variant="danger"
                onClick={() => removeShuttleBall(ball.id)}
              >
                -
              </Button>
            </Col>
          </Form.Group>
        ))}

        {/* Table existed services */}
        {tableServices.length !== 0 && (
          <>
            <Row className="justify-content-start" id="cus-table">
              <Col xs={6}>
                <FormLabel>Dich vu</FormLabel>
                <Table striped bordered hover size="sm">
                  <thead>
                    <tr>
                      <th>Service name</th>
                      <th>Service cost</th>
                    </tr>
                  </thead>
                  <tbody>
                    {tableServices.map((ser, idx) => (
                      <tr key={idx}>
                        <td>{ser.serviceName}</td>
                        <td>{ser.cost}</td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              </Col>
            </Row>
          </>
        )}

        {/* Add service area */}
        <Row className="row-space">
          <Col>
            <Button variant="success" onClick={handleAdd} className="me-2">
              + Add Service
            </Button>
          </Col>
        </Row>
        {services.map((service, index) => (
          <Row key={service.id} className="align-items-center mb-2">
            <Col sm={5}>
              <Form.Control
                type="text"
                placeholder="Service Name"
                value={service.serviceName}
                onChange={(e) =>
                  handleServiceChange(service.id, "serviceName", e.target.value)
                }
                required
              />
            </Col>
            <Col sm={4}>
              <Form.Control
                type="number"
                placeholder="Cost"
                value={service.cost}
                onChange={(e) =>
                  handleServiceChange(service.id, "cost", e.target.value)
                }
                required
              />
            </Col>
            <Col sm={1}>
              <Button variant="danger" onClick={() => handleRemove(service.id)}>
                -
              </Button>
            </Col>
          </Row>
        ))}
      </Form>
    </Container>
  );
}

export default SetupPage;
